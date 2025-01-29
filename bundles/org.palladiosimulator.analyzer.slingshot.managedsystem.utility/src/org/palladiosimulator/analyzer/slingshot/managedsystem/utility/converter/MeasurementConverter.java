package org.palladiosimulator.analyzer.slingshot.managedsystem.utility.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.measure.quantity.Duration;

import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.managedsystem.untility.converter.data.MeasurementSet;
import org.palladiosimulator.edp2.dao.MeasurementsDao;
import org.palladiosimulator.edp2.dao.exception.DataNotAccessibleException;
import org.palladiosimulator.edp2.models.ExperimentData.DataSeries;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentRun;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.ExperimentData.MeasurementRange;
import org.palladiosimulator.edp2.models.ExperimentData.RawMeasurements;
import org.palladiosimulator.edp2.util.MeasurementsUtility;
import org.palladiosimulator.edp2.util.MetricDescriptionUtility;
import org.palladiosimulator.metricspec.BaseMetricDescription;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.MonitorRepository;



/**
 * Static functions to extract information from different Palladio Objects
 *
 * Flats the beforehand 4 steps deep nesting to 2.
 *
 * @author Jonas Edlhuber
 *
 */
public class MeasurementConverter {
	public final static String POINT_IN_TIME = "Point in Time";
	public final static String RESPONSE_TIME = "Response Time";



	public static List<MeasurementSet> visitExperiementSetting(final ExperimentSetting es) {
		return es.getExperimentRuns().stream().map(x -> MeasurementConverter.visitExperimentRun(x)).flatMap(y -> y.stream()).toList();
	}

	public static List<MeasurementSet> visitExperimentRun(final ExperimentRun er) {
		return er.getMeasurement().stream().map(x -> MeasurementConverter.visitMeasurment(x)).flatMap(y -> y.stream()).toList();
	}

	public static List<MeasurementSet> visitMeasurment(final org.palladiosimulator.edp2.models.ExperimentData.Measurement m) {
		return m.getMeasurementRanges().stream().map(x -> MeasurementConverter.visitMeasurementRange(x)).flatMap(y -> y.stream()).toList();
	}

	public static List<MeasurementSet> visitMeasurementRange(final MeasurementRange mr) {
		if (mr.getRawMeasurements() == null)
			throw new IllegalStateException("No RawMeasurments found!");

		return MeasurementConverter.visitRawMeasurments(mr.getRawMeasurements());
	}

	/**
	 * This extracts the information of all data series from the raw measurement. It
	 * is asserted that the first data series contains the point in time and second
	 * the measured values.
	 *
	 * More that one data series with values is ignored at the moment!
	 *
	 * @param rawMeasurments
	 * @return
	 */
	public static List<MeasurementSet> visitRawMeasurments(final RawMeasurements rawMeasurments) {
		final List<MeasurementSet> measurments = new ArrayList<MeasurementSet>();

		if (rawMeasurments.getMeasurementRange().getMeasurement().getMeasuringType().getMetric().eContainer() == null)
			throw new IllegalStateException("MetricDescription not the one from common metrics.");

		for (int i = 0; i < rawMeasurments.getDataSeries().size(); i=i+2) {

			final var dataSeries1 = rawMeasurments.getDataSeries().get(i);
			final var dataSeries2 = rawMeasurments.getDataSeries().get(i+1);


			// it is presumed that the the index of the metric is correlated to the index of the data series
			final BaseMetricDescription bmc1 = MetricDescriptionUtility.toBaseMetricDescriptions(
					rawMeasurments.getMeasurementRange().getMeasurement().getMeasuringType().getMetric())[i];
			final BaseMetricDescription bmc2 = MetricDescriptionUtility.toBaseMetricDescriptions(
					rawMeasurments.getMeasurementRange().getMeasurement().getMeasuringType().getMetric())[i+1];

			final MeasurementSet ms = processDataSeries(dataSeries1, dataSeries2, bmc1, bmc2);


			if (ms != null)
				measurments.add(ms);
		}

		return measurments;
	}


	private static MeasurementSet processDataSeries(final DataSeries dataSeries1, final DataSeries dataSeries2, final BaseMetricDescription bmc1, final BaseMetricDescription bmc2) {
		final Predicate<? super MeasurementSpecification> filter = (x ->
			MetricDescriptionUtility.isBaseMetricDescriptionSubsumedByMetricDescription(bmc1, x.getMetricDescription()) ||
			MetricDescriptionUtility.isBaseMetricDescriptionSubsumedByMetricDescription(bmc2, x.getMetricDescription()));

		if (bmc1.getName().equals(POINT_IN_TIME)) {
			final var timestamp = MeasurementConverter.visitDataSeries(dataSeries1);
			return processDataSeries(timestamp, dataSeries2, filter);
		}
		else if (bmc2.getName().equals(POINT_IN_TIME)) {
			final var timestamp = MeasurementConverter.visitDataSeries(dataSeries2);
			return processDataSeries(timestamp, dataSeries1, filter);
		} else {
			return null;
		}

	}


	private static MeasurementSet processDataSeries(final List<Number> timestamps, final DataSeries dataSeries, final Predicate<? super MeasurementSpecification> filter) {
		final var measureName = dataSeries.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMeasuringPoint().getStringRepresentation();
		final var monitorRepo = Slingshot.getInstance().getInstance(MonitorRepository.class);
		final var mpoint = dataSeries.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMeasuringPoint();
		final var monitor = monitorRepo.getMonitors().stream().filter(x ->
            Objects.equals(x.getMeasuringPoint()
                .getResourceURIRepresentation(), mpoint.getResourceURIRepresentation()))
				.findAny().orElse(null);
		final var specification = monitor.getMeasurementSpecifications().stream()
				.filter(filter)
				.findAny().orElse(null);
		final var values = MeasurementConverter.visitDataSeries(dataSeries);

		if (timestamps.size() != values.size())
			throw new IllegalStateException("Number of point in time values and measurments value do not match!");

		final MeasurementSet ms = new MeasurementSet();
		final var md = specification.getMetricDescription();
		if(specification != null) {
			ms.setSpecificationId(specification.getId());
			ms.setSpecificationName(specification.getName());
			ms.setMonitorId(monitor.getId());
			ms.setMonitorName(monitor.getEntityName());
			ms.setName(measureName);
			ms.setMetricName(md.getName());
			ms.setMetricDescription(md.getTextualDescription());
		}

		for (int j = 0; j < timestamps.size(); j++) {
			ms.add(new MeasurementSet.Measurement<Number>(values.get(j), timestamps.get(j).doubleValue()));
		}

		return ms;
	}




	/**
	 * Visit a data series and return the double values in an array list.
	 *
	 * @param ds DataSeries with doubles
	 * @return ArrayList of Doubles
	 */
	private static List<Number> visitDataSeries(final DataSeries ds) {
		final var dao = (MeasurementsDao<Number, Duration>) MeasurementsUtility.<Duration>getMeasurementsDao(ds);

		final var measures = dao.getMeasurements();

		final var numbers = measures.stream().map(measure -> measure.getValue()).toList();

		try {
			dao.close();
		} catch (final DataNotAccessibleException e) {
			e.printStackTrace();
		}

		return numbers;
	}

}
