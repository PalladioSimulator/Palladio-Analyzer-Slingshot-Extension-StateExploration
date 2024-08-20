package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;

/**
 *
 * Triggers snapshot if a reconfiguration was triggered.
 *
 * @author Sarah Stieß
 *
 */
public class SnapshotTriggeringBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotTriggeringBehavior.class);

	private final Set<DESEvent> eventsToInitOn;
	private final DefaultState state;
	private final SimulationScheduling scheduling;

	private final boolean activated;

	@Inject
	public SnapshotTriggeringBehavior(final @Nullable DefaultState state,
			final @Nullable EventsToInitOnWrapper eventsToInitOn, final SimulationScheduling scheduling) {
		this.state = state;
		this.scheduling = scheduling;
		this.eventsToInitOn = eventsToInitOn.getEventsToInitOn();

		this.activated = state != null && eventsToInitOn != null;
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	@PreIntercept
	public InterceptionResult preInterceptModelAdjustmentRequested(final InterceptorInformation information,
			final ModelAdjustmentRequested event) {
		// only intercept triggered adjustments. do not intercept snapped adjustments..
		// assumption: do not copy adjustor events from the FEL, i.e. the "first" adjustor is always from the snapshot.
		if (eventsToInitOn.contains(event)) {
			LOGGER.debug(String.format("Succesfully route %s to %s", event.getName(), information.getEnclosingType().get().getSimpleName()));
			return InterceptionResult.success();
		}

		state.addReasonToLeave(ReasonToLeave.reactiveReconfiguration);
		scheduling.scheduleEvent(new SnapshotInitiated(0, event));

		LOGGER.debug(String.format("Abort routing %s to %s", event.getName(), information.getEnclosingType().get().getSimpleName()));
		return InterceptionResult.abort();
	}
}
