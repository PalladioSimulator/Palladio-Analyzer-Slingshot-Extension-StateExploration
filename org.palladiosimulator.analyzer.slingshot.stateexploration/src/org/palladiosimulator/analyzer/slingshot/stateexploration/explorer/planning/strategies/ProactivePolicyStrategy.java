package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies;

import java.util.List;

import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraphFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ToDoChange;

import com.google.common.base.Preconditions;

/**
 *
 * @author Sophie Stieß
 *
 */
public abstract class ProactivePolicyStrategy {

	protected final DefaultGraph graph;
	protected final DefaultGraphFringe fringe;

	/**
	 * Create new {@link ProactivePolicyStrategy}.
	 *
	 *
	 * @param graph  graph of the exploration, must not be {@code null}.
	 * @param fringe fringe of the exploration, must not be {@code null}.
	 */
	protected ProactivePolicyStrategy(final DefaultGraph graph, final DefaultGraphFringe fringe) {
		this.graph = Preconditions.checkNotNull(graph);
		this.fringe = Preconditions.checkNotNull(fringe);
	}

	/**
	 *
	 * Create proactive reconfigurations based on the given {@link DefaultState}.
	 *
	 * Ensures, that all of the {@link ToDoChange}s in the resulting list are yet
	 * unexplored. I.e. neither is any of them is in the state graphs fringe, nor
	 * has any of them already been explored.
	 *
	 * @return
	 */
	public abstract List<ToDoChange> createProactiveChanges();

}
