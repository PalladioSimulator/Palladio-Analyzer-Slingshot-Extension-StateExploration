package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import java.util.List;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;

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
public class FocusOnStatesEvent extends AbstractExplorationControllerEvent {

	private final List<RawModelState> path;

	/**
	 *
	 * @param path non-null, non-empty sequence of states.
	 */
	public FocusOnStatesEvent(final List<RawModelState> path) {
		super();
		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException(
					String.format("Path must not be null or empty, but is %s.", path == null ? "null" : "empty"));
		}
		this.path = List.copyOf(path);
	}

	public List<RawModelState> getPath() {
		return path;
	}
}
