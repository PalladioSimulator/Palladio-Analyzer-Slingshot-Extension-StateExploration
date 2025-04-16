package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.cost.events.TakeCostMeasurement;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotTaken;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;

/**
 *
 * This behaviour is responsible for things that relate to the initial adjustment of a simulation run.
 * 
 * Beware, the initial adjustment
 *  
 * 
 * Additional handling in case of starting simulation with an adjustment?
 * 
 * TODO : i think there was a bug wrt. cost in case of starting with adaption.
 * 
 * @author Sophie Stieß
 *
 */
@OnEvent(when = ModelAdjusted.class, then = { TakeCostMeasurement.class }, cardinality = EventCardinality.MANY)
@OnEvent(when = SnapshotInitiated.class, then = { TakeCostMeasurement.class }, cardinality = EventCardinality.MANY)
public class SnapshotCostMeasurementsBehaviour implements SimulationBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(SnapshotCostMeasurementsBehaviour.class);

	private final boolean activated;
	private final boolean startWithAdaption;

	/* for handling cost measurements */
	Collection<TakeCostMeasurement> costMeasurementStore = new ArrayList<>();
	private boolean handleCosts = true;

	@Inject
	public SnapshotCostMeasurementsBehaviour(final @Nullable EventsToInitOnWrapper eventsWrapper) {
		this.startWithAdaption = eventsWrapper != null && !eventsWrapper.getAdjustmentEvents().isEmpty();
		this.activated = eventsWrapper != null;
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	/**
	 *
	 * Intercept {@link TakeCostMeasurement} events.
	 *
	 * For two reasons: Firstly, get to know all resources with cost measures to
	 * trigger a final measurement in case of a snapshot. Secondly, abort the
	 * events, if the state starts with an adaptation. In this case, cost must be held back and only
	 * be measured after the adaptation, c.f.
	 * {@link SnapshotCostMeasurementsBehaviour#onModelAdjusted(ModelAdjusted)}
	 *
	 * @param information
	 * @param event
	 * @return success, if this state starts without adaptation, abort other wise.
	 */
	@PreIntercept
	public InterceptionResult preInterceptTakeCostMeasurement(final InterceptorInformation information,
			final TakeCostMeasurement event) {

		if (handleCosts && event.time() == 0) {
			costMeasurementStore.add(event);

			if (startWithAdaption) {
				return InterceptionResult.abort();
			}
		}
		return InterceptionResult.success();
	}

	/**
	 * When a Snapshot is initiated, trigger one final measurement for all cost
	 * monitors.
	 * 
	 * As a result, the intervals between the last and second to last cost
	 * measurments are smaller than the intervals defined in the cost model.
	 * 
	 * This is a certain inconsistency, but poses to significant advantages:
	 * <li>Need not save, republish and offset {@link TakeCostMeasurement} events
	 * between two simulation runs.
	 * <li>Every simulation run results in at least two cost measurements (at t=0
	 * and t = t_snapshot) which simplifies the calculation of the state utility
	 * later on.
	 * 
	 * This works independently of the order of reception of the
	 * {@link SnapshotInitiated} event. Costs are saved as part of the experiment
	 * settings, i.e. no need to ensure that the {@link TakeCostMeasurement} events
	 * are received <i>before</i> the {@link SnapshotTaken} event. It is sufficient,
	 * if the {@link TakeCostMeasurement} events are received before the
	 * {@link SimulationFinished} event, which is currentl guaranteed (only
	 * implicitly though, iirc).
	 * 
	 * @param event
	 * @return Set of events to trigger a final measurement for all cost monitors.
	 */
	@Subscribe
	public Result<TakeCostMeasurement> onSnapshotInitiated(final SnapshotInitiated event) {
		this.handleCosts = false;
		return Result.of(costMeasurementStore);
	}

	/**
	 * Publish {@link TakeCostMeasurement} events that were held back earlier.
	 *
	 * @param modelAdjusted adjustment and resulting changes that just happened.
	 */
	@Subscribe
	public Result<TakeCostMeasurement> onModelAdjusted(final ModelAdjusted modelAdjusted) {
		return Result.of(costMeasurementStore);
	}
}
