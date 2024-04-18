package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Quantity;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.monitor.data.entities.SlingshotMeasuringValue;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementUpdated;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementUpdated.MeasurementUpdateInformation;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.edp2.dao.MeasurementsDao;
import org.palladiosimulator.edp2.models.ExperimentData.DataSeries;
import org.palladiosimulator.edp2.models.ExperimentData.Measurement;
import org.palladiosimulator.edp2.models.ExperimentData.MeasurementRange;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.util.MeasurementsUtility;
import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;

/**
 *
 * TODO
 *
 * @author stiesssh
 *
 */
@OnEvent(when = MeasurementUpdated.class, then = SnapshotInitiated.class)
public class SnapshotSLOTriggeringBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotSLOTriggeringBehavior.class);

	private final DefaultState state;
	private final SimulationScheduling scheduling;

	private final ServiceLevelObjectiveRepository sloRepo;

	private final int minNumberOfMeasurementsForAvg = 5;

	private final boolean activated;

	/* Calculate mapping to save time later on (probably) */
	private final Map<MeasuringPoint, Set<ServiceLevelObjective>> mp2slo;
	private final Map<MeasuringPoint, Set<ValueRange>> mp2range;

	@Inject
	public SnapshotSLOTriggeringBehavior(final @Nullable DefaultState state,
			final @Nullable ServiceLevelObjectiveRepository sloRepo, final SimulationScheduling scheduling) {
		this.state = state;
		this.scheduling = scheduling;
		this.sloRepo = sloRepo;

		// TODO
		final double significance = 0.8;

		this.activated = this.state != null && this.sloRepo != null
				&& !this.sloRepo.getServicelevelobjectives().isEmpty();

		this.mp2slo = new HashMap<>();
		for (final ServiceLevelObjective slo : sloRepo.getServicelevelobjectives()) {

			if (slo.getLowerThreshold() == null && slo.getUpperThreshold() == null) {
				LOGGER.debug(
						String.format("No thresholds for %s [%s], will be ignored", slo.getName(), slo.getId()));
				continue;
			}
			if (slo.getLowerThreshold() != null && slo.getUpperThreshold() == null) {
				LOGGER.debug(
						String.format("No upper threshold for %s [%s], will be ignored", slo.getName(), slo.getId()));
				continue;
			}

			final MeasuringPoint mp = slo.getMeasurementSpecification().getMonitor().getMeasuringPoint();
			if (!mp2slo.containsKey(mp)) {
				mp2slo.put(mp, new HashSet<>());
			}
			mp2slo.get(mp).add(slo);
		}

		this.mp2range = new HashMap<>();
		for (final ServiceLevelObjective slo : sloRepo.getServicelevelobjectives()) {

			if (slo.getLowerThreshold() == null && slo.getUpperThreshold() == null) {
				LOGGER.debug(
						String.format("No thresholds for %s [%s], will be ignored", slo.getName(), slo.getId()));
				continue;
			}
			if (slo.getLowerThreshold() != null && slo.getUpperThreshold() == null) {
				LOGGER.debug(
						String.format("No upper threshold for %s [%s], will be ignored", slo.getName(), slo.getId()));
				continue;
			}

			final MeasuringPoint mp = slo.getMeasurementSpecification().getMonitor().getMeasuringPoint();
			if (!mp2range.containsKey(mp)) {
				mp2range.put(mp, new HashSet<>());
			}
			if (slo.getLowerThreshold() == null) {
				mp2range.get(mp).add(new SingleEndedRange(
						(Measure<Object, Quantity>) slo.getUpperThreshold().getThresholdLimit(), significance));
			} else {
				mp2range.get(mp)
				.add(new DoubleEndedRange(
						(Measure<Object, Quantity>) slo.getUpperThreshold().getThresholdLimit(),
						(Measure<Object, Quantity>) slo.getLowerThreshold().getThresholdLimit(), significance));
			}
		}
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	@Subscribe
	public Result<SnapshotInitiated> onMeasurementUpdated(final MeasurementUpdated event) {
		final MeasurementUpdateInformation info = event.getEntity();

		final MeasuringValue mval = info.getMeasuringValue();

		final List<Measure<?, ?>> list = mval.asList();

		final Measure<Double, Quantity> value = info.getMeasuringValue().getMeasureForMetric(info.getMeasuringValue().getMetricDesciption());
		final double calculationValue = value.doubleValue(value.getUnit());

		final MeasuringPoint point = info.getMeasuringPoint();

		final Set<ValueRange> slos = mp2range.getOrDefault(point, Set.of());

		for (final ValueRange slo : slos) {
			if (slo.stop(calculationValue)) {
				state.setReasonToLeave(ReasonToLeave.significantChange);
				return Result.of(new SnapshotInitiated(0.0));
			}
		}

		return Result.empty();
	}

	private abstract class ValueRange {
		protected final double significance;
		protected final double upper;
		protected final double lower;

		public ValueRange(final Measure<?, Quantity> upper, final Measure<?, Quantity> lower,
				final double significance) {
			this.significance = significance;

			final double u = upper.doubleValue(upper.getUnit());
			final double l = lower.doubleValue(lower.getUnit());

			final double middle = (u - l) / 2.0;

			this.upper = u - this.significance * middle;
			this.lower = l + this.significance * middle;
		}

		public abstract boolean stop(final double value);
	}

	private class DoubleEndedRange extends ValueRange {


		public DoubleEndedRange(final Measure<Object, Quantity> upper, final Measure<Object, Quantity> lower,
				final double significance) {
			super(upper, lower, significance);
		}

		@Override
		public boolean stop(final double value) {
			return value > this.upper || value < this.lower;
		}
	}

	private class SingleEndedRange extends ValueRange {

		public SingleEndedRange(final Measure<Object, Quantity> upper, final double significance) {
			super(upper, (Measure<Double, Quantity>) (Measure<Double, ?>) Measure.valueOf(0.0, Dimensionless.UNIT),
					significance);
		}

		@Override
		public boolean stop(final double value) {
			return value > this.upper;
		}
	}



	/**
	 * this is fake, but at least it is ;)
	 * @param value
	 * @return
	 */
	private boolean signicifantChange(final SlingshotMeasuringValue value) {
		final Optional<Measurement> measurementMatch = this.match(value.getMeasuringPoint());

		if (measurementMatch.isEmpty()) {
			return false;
		}
		if (!this.canAverage(measurementMatch.get())) {
			return false;
		}

		final Double firstValue = this.extractFirstValue(measurementMatch.get());

		final Measure<Double, Duration> newMeasure = (Measure<Double, Duration>) value.asList().get(1);
		final Double newValue = newMeasure.doubleValue(newMeasure.getUnit());

		// TODO make better condition.
		if (Math.abs(newValue) > Math.abs(firstValue) * 3) {
			state.setDecreaseInterval(true);
			return true;
		}
		return false;
	}

	private Double extractFirstValue(final Measurement measurement) {
		final List<MeasurementRange> range = measurement.getMeasurementRanges();
		if (range.size() != 1) {
			throw new IllegalArgumentException("i fucked up");
		}

		final List<DataSeries> series = range.get(0).getRawMeasurements().getDataSeries();

		final Double averageFirstValue  = this.averageOrSomething(series.get(0), series.get(1));

		return averageFirstValue;
	}


	private boolean canAverage(final Measurement measurement) {
		final List<MeasurementRange> range = measurement.getMeasurementRanges();
		if (range.size() != 1) {
			throw new IllegalArgumentException("i fucked up");
		}

		final DataSeries series = range.get(0).getRawMeasurements().getDataSeries().get(0);

		return MeasurementsUtility.<Duration> getMeasurementsDao(series).getMeasurements().size() > this.minNumberOfMeasurementsForAvg;
	}

	/**
	 *
	 * @param point
	 * @return
	 */
	private Optional<Measurement> match(final MeasuringPoint point) {
		final List<Measurement> measurements = state.getExperimentSetting().getExperimentRuns().get(0).getMeasurement();

		final List<Measurement> measurementMatches = measurements.stream().filter(m -> m.getMeasuringType().getMeasuringPoint().getStringRepresentation().equals(point.getStringRepresentation())).collect(Collectors.toList());

		if (measurementMatches.size() == 1) {
			return Optional.of(measurementMatches.get(0));
		}
		return Optional.empty();
	}

	/**
	 *	let's average the first 5 values..
	 *
	 * @param time
	 * @param value
	 * @return
	 */
	private double averageOrSomething(final DataSeries time, final DataSeries value) {

		//final List<Measure<Double, Duration>> times = ((MeasurementsDao<Double, Duration>) MeasurementsUtility.<Duration> getMeasurementsDao(time)).getMeasurements();
		final List<Measure<Double, Duration>> values = ((MeasurementsDao<Double, Duration>) MeasurementsUtility.<Duration> getMeasurementsDao(value)).getMeasurements();

		//final double minTime = times.stream().mapToDouble(t -> t.doubleValue(t.getUnit())).min().getAsDouble();
		//final long minTimeCount = times.stream().mapToDouble(t -> t.doubleValue(t.getUnit())).filter(t -> t == minTime).count();

		double sum = 0.0;
		for (int i = 0; i < minNumberOfMeasurementsForAvg; i++) {
			sum += values.get(i).doubleValue(values.get(i).getUnit());
		}
		final double average = sum / minNumberOfMeasurementsForAvg;

		return average;
	}
}
