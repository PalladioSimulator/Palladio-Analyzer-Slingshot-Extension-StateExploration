package org.palladiosimulator.analyzer.slingshot.managedsystem.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.measure.quantity.Duration;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.managedsystem.export.data.IdentifieableMeasurements;
import org.palladiosimulator.analyzer.slingshot.managedsystem.export.data.Interval;
import org.palladiosimulator.analyzer.slingshot.managedsystem.export.data.MeasurementPair;
import org.palladiosimulator.analyzer.slingshot.managedsystem.export.data.MeasurementPairIdentifier;
import org.palladiosimulator.analyzer.slingshot.managedsystem.export.data.MeasurementsExportedData;
import org.palladiosimulator.analyzer.slingshot.managedsystem.export.messages.MeasurementsExported;
import org.palladiosimulator.analyzer.slingshot.managedsystem.export.messages.MeasurementsRequested;
import org.palladiosimulator.analyzer.slingshot.networking.data.NetworkingConstants;
import org.palladiosimulator.edp2.dao.MeasurementsDao;
import org.palladiosimulator.edp2.dao.exception.DataNotAccessibleException;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.ExperimentData.DataSeries;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentRun;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.ExperimentData.Measurement;
import org.palladiosimulator.edp2.models.ExperimentData.MeasurementRange;
import org.palladiosimulator.edp2.models.ExperimentData.MeasuringType;
import org.palladiosimulator.edp2.models.ExperimentData.RawMeasurements;
import org.palladiosimulator.edp2.models.Repository.Repositories;
import org.palladiosimulator.edp2.models.Repository.Repository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.util.MeasurementsUtility;
import org.palladiosimulator.edp2.util.MetricDescriptionUtility;
import org.palladiosimulator.metricspec.BaseMetricDescription;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.metricspec.MetricSetDescription;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.MonitorRepository;

import com.google.inject.ProvisionException;

/**
 *
 * Exports measurements of the managed system on request.
 *
 * @author Sarah Stieß
 */
@OnEvent(when = MeasurementsRequested.class, then = { MeasurementsExported.class })
public class ExportSystemBehaviour implements SystemBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(ExportSystemBehaviour.class);

    private final String clientName;

    private final Map<MeasurementPairIdentifier, MeasurementRange> map = new HashMap<>();

    @Inject
    public ExportSystemBehaviour(@Named(NetworkingConstants.CLIENT_NAME) final String clientName) {
        this.clientName = clientName;
    }

    /**
     *
     * @param event
     */
    @Subscribe
    public void onMeasurementsRequested(final MeasurementsRequested event) {

        if (map.isEmpty()) {
            final List<MeasurementSpecification> specs = getMeasurementSpecifications();
            final List<Measurement> measurements = getMeasurements();

            for (final MeasurementSpecification spec : specs) {

                final Optional<Measurement> match = findMatchInList(spec, measurements);

                if (match.isPresent()) {
                    final MeasurementRange range = getElement(match.get(), Measurement::getMeasurementRanges);
                    final MeasurementPairIdentifier identifier = MeasurementPairIdentifier.of(spec);

                    map.put(identifier, range);
                }
            }
        }

        final List<IdentifieableMeasurements> rvals = new ArrayList<>();

        for (final Map.Entry<MeasurementPairIdentifier, MeasurementRange> entry : map.entrySet()) {

            final List<MeasurementPair<Number>> cutSection = cutSection(entry.getValue(), event.getPayload());
            rvals.add(new IdentifieableMeasurements(entry.getKey(), cutSection));
        }

        Slingshot.getInstance()
            .getSystemDriver()
            .postEvent(new MeasurementsExported(new MeasurementsExportedData(rvals, event.getPayload()), clientName));
    }

    /**
     *
     * Finds a {@link Measurement} matching the given {@link MeasurementSpecification} in the given
     * collection of measurements.
     *
     * Beware, if the monitor repository contains duplicated specifications (i.e. same
     * {@link MeasuringPoint} and same {@link MetricDescription}), there might be multiple matching
     * measurements, but only one of them holds values. In this case, the one with values is
     * selected. If none holds values, any is selected.
     *
     * @param spec
     *            the specification to find a matching measurement for.
     * @param measurements
     *            all measurements from the EDP2 repository.
     * @return a matching {@link Measurement} for, or an empty optional if none exists.
     */
    private static Optional<Measurement> findMatchInList(final MeasurementSpecification spec,
            final Collection<Measurement> measurements) {

        final List<Measurement> matchees = measurements.stream()
            .filter(measurement -> isMatch(measurement, spec))
            .toList();

        if (matchees.size() == 1) {
            return Optional.of(matchees.get(0));
        } else if (matchees.isEmpty()) {
            return Optional.empty();
        } else {
            final List<Measurement> matcheesWithValues = matchees.stream()
                .filter(ExportSystemBehaviour::hasValues)
                .toList();

            if (matcheesWithValues.size() > 1) {
                LOGGER.warn(String.format(
                        "Too many matching measurements for spec %s[%s]. Expected at max 1 measuremnt with values, but found %d.",
                        spec.getName(), spec.getId(), matcheesWithValues.size()));
            }
            if (matcheesWithValues.isEmpty()) {
                return Optional.of(matchees.get(0));
            } else {
                return Optional.of(matcheesWithValues.get(0));
            }
        }

    }

    /**
     *
     * Check whether a {@link Measurement} and a {@link MeasurementSpecification} match.
     *
     * They are considered matching, if they reference the same {@link MeasuringPoint} and if the
     * {@link BaseMetricDescription} of the specification is subsumed by the
     * {@link MetricSetDescription} of the measurement.
     *
     * @param measurement
     * @param spec
     * @return true if the {@link Measurement} and the {@link MeasurementSpecification} match, false
     *         otherwise.
     */
    private static boolean isMatch(final Measurement measurement, final MeasurementSpecification spec) {

        final MeasuringType type = measurement.getMeasuringType();

        final boolean descmatches = MetricDescriptionUtility.isBaseMetricDescriptionSubsumedByMetricDescription(
                (BaseMetricDescription) spec.getMetricDescription(), type.getMetric());

        final boolean mpmatches = spec.getMonitor()
            .getMeasuringPoint()
            .getStringRepresentation()
            .equals(type.getMeasuringPoint()
                .getStringRepresentation());

        return mpmatches && descmatches;
    }

    /**
     *
     * @param measurement
     * @return true iff the given {@link Measurement} contains any values, false otherwise.
     */
    private static boolean hasValues(final Measurement measurement) {

        final MeasurementRange range = getElement(measurement, Measurement::getMeasurementRanges);

        final DataSeries ds = getElement(range.getRawMeasurements(), RawMeasurements::getDataSeries);

        final MeasurementsDao<?, ?> dao = MeasurementsUtility.getMeasurementsDao(ds);

        final List<?> measures = dao.getMeasurements();

        try {
            dao.close();
        } catch (final DataNotAccessibleException e) {
            e.printStackTrace();
        }

        return !measures.isEmpty();
    }



    /**
     *
     * Get the measurement specifications.
     *
     * Accesses the monitor repository that contain the specs via Slingshot's injection mechanism.
     * If the simulation has not yet started, no monitoring repository can be provisioned.
     *
     * @return the measurement specifications, or an empty list if the monitor repository is not yet
     *         provisionable.
     */
    private static List<MeasurementSpecification> getMeasurementSpecifications() {
        try {

            final MonitorRepository repo = Slingshot.getInstance()
                .getInstance(MonitorRepository.class);

            return repo.getMonitors()
                .stream()
                .flatMap(monitor -> monitor.getMeasurementSpecifications()
                    .stream())
                .toList();

        } catch (final ProvisionException e) {
            LOGGER.info(String.format("Cannot access MonitorRepository, because %s.", e.getMessage()));
            LOGGER.debug(e.getStackTrace());
        }
        return List.of();
    }

    /**
     * Nice an short code. i wonder whether it still works.
     *
     * @return
     */
    private static List<Measurement> getMeasurements() {
        final Repository repo = getElement(RepositoryManager.getCentralRepository(),
                Repositories::getAvailableRepositories);
        final ExperimentGroup group = getElement(repo, Repository::getExperimentGroups);
        final ExperimentSetting setting = getElement(group, ExperimentGroup::getExperimentSettings);
        final ExperimentRun run = getElement(setting, ExperimentSetting::getExperimentRuns);

        return run.getMeasurement();
    }

    /**
     *
     * Get the first element of an one-to-many containment relation, if at least one object is in
     * the containment.
     *
     * @param <T>
     *            type of the content to access.
     * @param <R>
     *            type of the container.
     * @param container
     *            the container that contains the content, must not be {@code null}.
     * @param getter
     *            function to get an {@link EList} full of content of type {@code T} from the
     *            container, must not be {@code null}.
     * @throws IllegalArgumentException
     *             if the containment is empty.
     * @return get the element.
     */
    private static <T extends EObject, R extends EObject> T getElement(final R container,
            final Function<R, EList<T>> getter) {

        if (container == null || getter == null) {
            throw new IllegalArgumentException("null arguments are not allowed.");
        }

        final EList<T> content = getter.apply(container);

        if (content.isEmpty()) {
            throw new IllegalArgumentException(String.format("Content must not be empty, but is.", content.size()));
        }
        if (content.size() > 1) {
            LOGGER.debug(String.format("Size of content is %d. You might want to check on that.", content.size()));
        }
        return content.get(0);
    }

    /**
     * Visit a data series and return the double values in an array list.
     *
     * @param ds
     *            DataSeries with doubles
     * @return ArrayList of Doubles
     */
    private static List<Number> visitDataSeries(final DataSeries ds) {
        final MeasurementsDao<Number, Duration> dao = (MeasurementsDao<Number, Duration>) MeasurementsUtility
            .<Duration> getMeasurementsDao(ds);

        final List<Number> numbers = dao.getMeasurements()
            .stream()
            .map(measure -> measure.getValue())
            .toList();

        try {
            dao.close();
        } catch (final DataNotAccessibleException e) {
            e.printStackTrace();
        }

        return numbers;
    }

    /**
     *
     * Extract measurements from the given {@link MeasurementRange} into a list of time-value pairs.
     * Only includes pairs, where the point in time is inside the given interval.
     *
     * @param range
     * @param interval
     *            interval to cut
     * @return time-value pairs, that are inside the given interval.
     */
    private static List<MeasurementPair<Number>> cutSection(final MeasurementRange range, final Interval interval) {
        // assumption: index of metric correlates to the index of data series
        final BaseMetricDescription bmc1 = MetricDescriptionUtility.toBaseMetricDescriptions(range.getMeasurement()
            .getMeasuringType()
            .getMetric())[0];

        // (i dont even know whether this is alright because of concurrency... )

        final List<Number> series0 = ExportSystemBehaviour.visitDataSeries(range.getRawMeasurements()
            .getDataSeries()
            .get(0));
        final List<Number> series1 = ExportSystemBehaviour.visitDataSeries(range.getRawMeasurements()
            .getDataSeries()
            .get(1));

        if (bmc1.equals(MetricDescriptionConstants.POINT_IN_TIME_METRIC)) {
            return cutSection(series0, series1, interval.lowerBound(), interval.upperBound());
        } else {
            return cutSection(series1, series0, interval.lowerBound(), interval.upperBound());
        }
    }

    /**
     *
     * Merge measurements from the given time and value measurement series into a list of time-value
     * pairs. Only includes pairs, where the time measurement is inside the interval defined by
     * {@code lowerTime} and {@code upperTime}.
     *
     *
     * @param timeSeries
     *            time measurements
     * @param valueSeries
     *            value measurements
     * @param lowerTime
     *            lower limit
     * @param upperTime
     *            upper limit
     * @return time-value pairs, that are inside the interval defined by the given bounds.
     */
    private static List<MeasurementPair<Number>> cutSection(final List<Number> timeSeries,
            final List<Number> valueSeries,
            final double lowerTime, final double upperTime) {
        assert lowerTime >= 0 : "lower bound is negative, but must not be.";
        assert upperTime >= lowerTime : "lower bound is smaller than upper bound, but must not be.";

        final List<MeasurementPair<Number>> rvals = new ArrayList<>();

        for (int i = 0; i < timeSeries.size(); i++) {
            final double timeValue = timeSeries.get(i)
                .doubleValue();

            if (timeValue >= lowerTime && timeValue < upperTime) {
                rvals.add(new MeasurementPair<Number>(timeValue, valueSeries.get(i)));
            }
        }

        return rvals;
    }

}
