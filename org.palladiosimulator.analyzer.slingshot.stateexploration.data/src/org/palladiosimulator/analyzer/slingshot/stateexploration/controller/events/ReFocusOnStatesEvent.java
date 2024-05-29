package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import java.util.Collection;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ToDoChange;

/**
 *
 * Event to make the {@link GraphExplorer} refocus the exploration on the given
 * states.
 *
 * A state is <i>out of focus</i> if there are no {@link ToDoChange}s for the
 * state in the fringe. A state might get <i>out of focus</i> for three reasons:
 * <li>It is fully explored
 * <li>The Exploration focused on other events due to a
 * {@link FocusOnStatesEvent}.
 * <li>The state is in the past, compared to the execution of the actual system.
 *
 *
 * @author Sarah Stieß
 *
 */
public class ReFocusOnStatesEvent extends EventMessage<Collection<String>>
implements ExplorationControllerEvent {

	public static final String MESSAGE_MAPPING_IDENTIFIER = ReFocusOnStatesEvent.class.getSimpleName();

	/**
	 *
	 * @param focusStateIds non-null, non-empty collection of state ids.
	 */
	public ReFocusOnStatesEvent(final Collection<String> focusStateIds) {
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
