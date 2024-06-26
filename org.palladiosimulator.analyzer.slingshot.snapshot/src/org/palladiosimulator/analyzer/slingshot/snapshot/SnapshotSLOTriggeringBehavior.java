package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Quantity;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementUpdated;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;

/**
 *
 * Triggers snapshots based on a measurements closeness to defined SLOs.
 *
 * This class does <strong>not</strong> use the raw measurements provided by
 * {@code MeasurementMade} events. Instead it uses the aggregated values
 * provided by {@code MeasurementUpdated} events. Beware: The aggregated values
 * provided by {@code MeasurementUpdated} events are aggregated according to the
 * {@code ProcessingType} elements defined in the {@code MonitorRepository}.
 * This class does not aggregate on its own.
 *
 *
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when = MeasurementUpdated.class, then = SnapshotInitiated.class)
public class SnapshotSLOTriggeringBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotSLOTriggeringBehavior.class);

	private final DefaultState state;
	private final ServiceLevelObjectiveRepository sloRepo;

	private final boolean activated;

	/* Calculate mapping to save time later on (probably) */
	private final Map<MeasurementSpecification, Set<ValueRange>> mp2range;

	private final double sensitivity;
	private final double minDuration;

	@Inject
	public SnapshotSLOTriggeringBehavior(final @Nullable DefaultState state,
			final @Nullable ServiceLevelObjectiveRepository sloRepo, final @Nullable SnapshotConfiguration config) {

		this.activated = state != null && sloRepo != null && config != null
				&& !sloRepo.getServicelevelobjectives().isEmpty();

		this.state = state;
		this.sloRepo = sloRepo;

		this.minDuration = activated ? config.getMinDuration() : 0;
		this.sensitivity = activated ? config.getSensitivity() : 0;

		this.mp2range = new HashMap<>();

		if (activated) {
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

				final MeasurementSpecification mp = slo.getMeasurementSpecification();
				if (!mp2range.containsKey(mp)) {
					mp2range.put(mp, new HashSet<>());
				}
				if (slo.getLowerThreshold() == null) {
					mp2range.get(mp).add(new SingleEndedRange(
							(Measure<Object, Quantity>) slo.getUpperThreshold().getThresholdLimit(), sensitivity));
				} else {
					mp2range.get(mp)
					.add(new DoubleEndedRange(
							(Measure<Object, Quantity>) slo.getUpperThreshold().getThresholdLimit(),
							(Measure<Object, Quantity>) slo.getLowerThreshold().getThresholdLimit(), sensitivity));
				}
			}
		}
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	@Subscribe
	public Result<SnapshotInitiated> onMeasurementUpdated(final MeasurementUpdated event) {

		if (event.time() < minDuration
				|| !mp2range.containsKey(event.getEntity().getProcessingType().getMeasurementSpecification())) {
			return Result.empty();
		}

		final MetricDescription base = event.getEntity().getProcessingType().getMeasurementSpecification()
				.getMetricDescription();

		final Measure<Double, Quantity> value = event.getEntity().getMeasuringValue().getMeasureForMetric(base);

		final double calculationValue = value.doubleValue(value.getUnit());

		final MeasurementSpecification spec = event.getEntity().getProcessingType().getMeasurementSpecification();

		for (final ValueRange range : mp2range.get(spec)) {
			if (range.isViolatedBy(calculationValue)) {
				state.setReasonToLeave(ReasonToLeave.closenessToSLO);
				this.mp2range.clear(); // reset to avoid additional Snapshot Initiations.
				return Result.of(new SnapshotInitiated(0.0));
			}
		}

		return Result.empty();
	}

	/**
	 * Value range with upper and lower sensibility bound. Bounds are calculated
	 * based on the upper and lower thresholds of the SLO and the provided
	 * sensibility.
	 *
	 * If a measured value is greater than the upper bound, or smaller than the
	 * lower bound, the range is considered violated and the simulation run should
	 * stop.
	 *
	 *
	 * @author Sarah Stieß
	 *
	 */
	private abstract class ValueRange {
		protected final double sensitivity;
		protected final double upper;
		protected final double lower;

		/**
		 *
		 * @param upper       Upper Threshold of SLO
		 * @param lower       Lower Threshold of SLO
		 * @param sensitivity Number in [0,1] where 0 is insensible, and 1 is very
		 *                    sensible.
		 */
		public ValueRange(final Measure<?, Quantity> upper, final Measure<?, Quantity> lower,
				final double sensitivity) {
			this.sensitivity = sensitivity;

			final double u = upper.doubleValue(upper.getUnit());
			final double l = lower.doubleValue(lower.getUnit());

			final double middle = (u - l) / 2.0;

			this.upper = u - this.sensitivity * middle;
			this.lower = l + this.sensitivity * middle;
		}

		/**
		 * Determines, whether {@code value} violates this value range.
		 *
		 * @param value
		 * @return true, if this range is violated, false otherwise.
		 */
		public abstract boolean isViolatedBy(final double value);
	}

	/**
	 * Value range with upper and lower sensibility bound.
	 *
	 * @author Sarah Stieß
	 *
	 */
	private class DoubleEndedRange extends ValueRange {

		/**
		 * @see {@link ValueRange}
		 */
		public DoubleEndedRange(final Measure<Object, Quantity> upper, final Measure<Object, Quantity> lower,
				final double significance) {
			super(upper, lower, significance);
		}

		@Override
		public boolean isViolatedBy(final double value) {
			return value >= this.upper || value <= this.lower;
		}
	}

	/**
	 * Value range with only an upper sensibility bound. For calulation, the lower
	 * bound i treated as zero. For checks, only the upper bound is considered.
	 *
	 * @author Sarah Stieß
	 *
	 */
	private class SingleEndedRange extends ValueRange {

		/**
		 * Defaults to 0 as lower threshold.
		 *
		 * @see {@link ValueRange}
		 */
		public SingleEndedRange(final Measure<Object, Quantity> upper, final double significance) {
			super(upper, (Measure<Double, Quantity>) (Measure<Double, ?>) Measure.valueOf(0.0, Dimensionless.UNIT),
					significance);
		}

		@Override
		public boolean isViolatedBy(final double value) {
			return value >= this.upper;
		}
	}
}
