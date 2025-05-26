package org.palladiosimulator.analyzer.slingshot.snapshot;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.usageevolution.events.IntervalPassed;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.cost.events.TakeCostMeasurement;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PostIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.ExploredStateBuilder;

/**
 * Behaviour to continuously offset the {@link IntervalPassed} events for the
 * usage evolution.
 * 
 * Keep in mind: All simulation runs start at t=0s, even if the state they
 * represent starts at t > 0s. Without offsetting the {@link IntervalPassed}
 * events we would always replay the first few seconds of the usage evolution,
 * which we clearly do not want. Instead we want to keep reading from the model
 * where we left of with the previous simulation run, thus we offset all events
 * for the usage evolution with the start time of the current state.
 * 
 * Another thing to keep in mind: this is only necessary for the usage
 * evolution, because the usage evolution model is the only time dependent
 * model.
 * 
 * @author Sophie Stieß
 *
 */
public class PreventPreemptiveSimulationFinished implements SimulationBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(PreventPreemptiveSimulationFinished.class);

	private final boolean activated;

	private boolean snapshotFinished = false;

	@Inject
	public PreventPreemptiveSimulationFinished(final @Nullable ExploredStateBuilder stateBuilder) {

		this.activated = stateBuilder != null;
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	/**
	 *
	 * Catch {@link TakeCostMeasurement} events (usage evolution) from the snapshot
	 * and offset them into the "future". Otherwise, we will get the wrong values
	 * from the Load Intensity model.
	 *
	 * @param information interception information
	 * @param event       intercepted event
	 * @return always success
	 */
	@PreIntercept
	public InterceptionResult preInterceptSimualtionFinished(final InterceptorInformation information,
			final SimulationFinished event) {
		if (this.snapshotFinished) {
			return InterceptionResult.success();
		} else {
			return InterceptionResult.abort();
		}
	}
	
	
	@PostIntercept
	public InterceptionResult postInterceptSnapshotFinished(final InterceptorInformation information,
			final SnapshotFinished event, final Result<?> result) {
		this.snapshotFinished = true;
		return InterceptionResult.success();
	}
}
