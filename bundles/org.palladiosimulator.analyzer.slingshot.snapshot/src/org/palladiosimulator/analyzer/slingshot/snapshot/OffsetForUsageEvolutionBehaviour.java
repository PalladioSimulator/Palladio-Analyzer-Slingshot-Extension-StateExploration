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
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultStateBuilder;
import org.scaledl.usageevolution.UsageEvolution;

/**
 *
 * Usage Evolution continuous offset. 
 *
 * @author Sophie Stieß
 *
 */
public class OffsetForUsageEvolutionBehaviour implements SimulationBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(OffsetForUsageEvolutionBehaviour.class);

	private final boolean activated;
	private final double startTime;

	@Inject
	public OffsetForUsageEvolutionBehaviour(final @Nullable DefaultStateBuilder halfDoneState, @Nullable final UsageEvolution usageEvolutionModel) {

		this.activated = halfDoneState != null && usageEvolutionModel != null;

		this.startTime = halfDoneState.getStartupInformation().startTime();
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
