package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import java.util.List;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
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
public class ReFocusOnStatesEvent extends FocusOnStatesEvent {

	/**
	 *
	 * @param path non-null, non-empty sequence of states.
	 */
	public ReFocusOnStatesEvent(final List<RawModelState> states) {
		super(states);
	}
}
