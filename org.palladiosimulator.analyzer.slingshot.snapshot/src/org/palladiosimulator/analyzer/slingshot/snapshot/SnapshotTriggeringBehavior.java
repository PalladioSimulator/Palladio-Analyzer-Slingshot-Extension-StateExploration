package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.measure.Measure;
import javax.measure.quantity.Duration;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
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
//@OnEvent(when = ModelAdjusted.class, then = SnapshotInitiated.class)
public class SnapshotTriggeringBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotTriggeringBehavior.class);

	private final DefaultState state;


	@Inject
	public SnapshotTriggeringBehavior(final DefaultState state) {
		this.state = state;
	}


	@Subscribe
	public Result<SnapshotInitiated> onMeasurementMade(final MeasurementMade event) {
		if (this.signicifantChange(event.getEntity())){
			state.setReasonToLeave(ReasonToLeave.significantChange);
			return Result.of(new SnapshotInitiated());
		}
		return Result.empty();
	}

//	@Subscribe
//	public Result<?> onModelAdjusted(final ModelAdjusted event) {
//		// not yet sure about this one, it causes lots of complications.
//		// Cannot go for "SnapshotInitiated", cause the snapshot must happen right now at this very moment.
//		// With the PreInterception form the new framework, i'd intercept the ModelAdjusted,
//		return Result.empty();
//	}


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
	 *
	 * @param time
	 * @param value
	 * @return
	 */
	private double averageOrSomething(final DataSeries time, final DataSeries value) {

		final List<Measure<Double, Duration>> times = ((MeasurementsDao<Double, Duration>) MeasurementsUtility.<Duration> getMeasurementsDao(time)).getMeasurements();
		final List<Measure<Double, Duration>> values = ((MeasurementsDao<Double, Duration>) MeasurementsUtility.<Duration> getMeasurementsDao(value)).getMeasurements();

		final double minTime = times.stream().mapToDouble(t -> t.doubleValue(t.getUnit())).min().getAsDouble();
		final long minTimeCount = times.stream().mapToDouble(t -> t.doubleValue(t.getUnit())).filter(t -> t == minTime).count();

		double sum = 0.0;
		for (int i = 0; i < minTimeCount; i++) {
			sum += values.get(i).doubleValue(values.get(i).getUnit());
		}
		final double average = sum / minTimeCount;

		return average;
	}
}
