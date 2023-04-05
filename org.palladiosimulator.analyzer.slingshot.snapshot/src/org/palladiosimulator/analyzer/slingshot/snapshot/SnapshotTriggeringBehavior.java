package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.measure.Measure;
import javax.measure.quantity.Duration;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.AdjustorBasedEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.monitor.data.entities.SlingshotMeasuringValue;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.edp2.dao.MeasurementsDao;
import org.palladiosimulator.edp2.models.ExperimentData.DataSeries;
import org.palladiosimulator.edp2.models.ExperimentData.Measurement;
import org.palladiosimulator.edp2.models.ExperimentData.MeasurementRange;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.util.MeasurementsUtility;

/**
 *
 * TODO
 *
 * @author stiesssh
 *
 */
@OnEvent(when = MeasurementMade.class, then = SnapshotInitiated.class)
public class SnapshotTriggeringBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotTriggeringBehavior.class);

	private final DefaultState state;
	private final SimulationScheduling scheduling;

	private final int minNumberOfMeasurementsForAvg = 5;


	@Inject
	public SnapshotTriggeringBehavior(final DefaultState state, final SimulationScheduling scheduling) {
		this.state = state;
		this.scheduling = scheduling;
	}


	@Subscribe
	public Result<SnapshotInitiated> onMeasurementMade(final MeasurementMade event) {
		if (this.signicifantChange(event.getEntity())){
			state.setReasonToLeave(ReasonToLeave.significantChange);
			return Result.of(new SnapshotInitiated());
		}
		return Result.empty();
	}

	@PreIntercept
	public InterceptionResult preInterceptSimulationStarted(final InterceptorInformation information,
			final AdjustorBasedEvent event) {
		// only intercept triggered adjustments. do not intercept snapped adjustments..
		// assumption: do not copy adjustor events from the FEL, i.e. the "first" adjustor is always from the snapshot.
		if (event.time() == 0) {
			LOGGER.debug(String.format("Succesfully route %s to %s", event.getName(), information.getEnclosingType().get().getSimpleName()));
			return InterceptionResult.success();
		}

		state.setReasonToLeave(ReasonToLeave.reactiveReconfiguration);
		scheduling.scheduleEvent(new SnapshotInitiated(0, event));

		LOGGER.debug(String.format("Abort routing %s to %s", event.getName(), information.getEnclosingType().get().getSimpleName()));
		return InterceptionResult.abort();
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
