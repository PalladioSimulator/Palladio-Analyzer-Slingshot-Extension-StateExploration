package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies;

import java.util.Collection;

import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ToDoChange;

public abstract class ProactivePolicyStrategy {

	/**
	 *
	 * Create proactive reconfigurations based on the given {@link DefaultState}.
	 *
	 * Ensures, that all of the {@link ToDoChange}s in the resulting list are yet
	 * unexplored. I.e. neither is any of them is in the state graphs fringe, nor
	 * has any of them already been explored.
	 *
	 * @param state predecessor for created changes.
	 * @return
	 */
	public abstract Collection<ToDoChange> createProactiveChanges(final DefaultState state);

}
