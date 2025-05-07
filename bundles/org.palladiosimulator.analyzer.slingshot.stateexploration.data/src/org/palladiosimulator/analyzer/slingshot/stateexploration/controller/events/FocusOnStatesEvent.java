package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import java.util.Collection;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 * Event to make the {@link GraphExplorer} focus the exploration on a given
 * path. The path to focus on would usually be the plan created by the planner.
 *
 * The path is a sequence of at least one state. There is no guarantee regarding
 * the completeness of the path. It may be the entire plan, or only a subset of
 * the planned states. In the end, this is decided by the planner component.
 *
 * @author Sarah Stieß
 *
 */
public class FocusOnStatesEvent extends EventMessage<Collection<String>>
implements ExplorationControllerEvent {

	public static final String MESSAGE_MAPPING_IDENTIFIER = FocusOnStatesEvent.class.getSimpleName();

	/**
	 *
	 * @param focusStateIds non-null, non-empty collection of state ids.
	 */
	public FocusOnStatesEvent(final Collection<String> focusStateIds) {
		super(MESSAGE_MAPPING_IDENTIFIER, focusStateIds);
		if (focusStateIds == null || focusStateIds.isEmpty()) {
			throw new IllegalArgumentException(
					String.format("Ids of states to focus on must not be null or empty, but is %s.",
							focusStateIds == null ? "null" : "empty"));
		}
	}

	public Collection<String> getFocusStateIds() {
		return Set.copyOf(this.getPayload());
	}
}
