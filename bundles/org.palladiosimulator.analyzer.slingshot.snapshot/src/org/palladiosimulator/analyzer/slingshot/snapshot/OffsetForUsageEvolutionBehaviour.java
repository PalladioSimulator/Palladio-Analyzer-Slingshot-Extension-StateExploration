package org.palladiosimulator.analyzer.slingshot.snapshot;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.usageevolution.events.IntervalPassed;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.cost.events.TakeCostMeasurement;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.ExploredStateBuilder;
import org.scaledl.usageevolution.UsageEvolution;

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
public class OffsetForUsageEvolutionBehaviour implements SimulationBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(OffsetForUsageEvolutionBehaviour.class);

	private final boolean activated;
	private final double startTime;

	@Inject
	public OffsetForUsageEvolutionBehaviour(final @Nullable ExploredStateBuilder stateBuilder,
			@Nullable final UsageEvolution usageEvolutionModel) {

		this.activated = stateBuilder != null && usageEvolutionModel != null
				&& stateBuilder.getStartupInformation().startTime() > 0;

		this.startTime = stateBuilder.getStartupInformation().startTime();
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
	public InterceptionResult preInterceptIntervalPassed(final InterceptorInformation information,
			final IntervalPassed event) {
		event.setTime(event.time() + this.startTime);
		return InterceptionResult.success();
	}
}
