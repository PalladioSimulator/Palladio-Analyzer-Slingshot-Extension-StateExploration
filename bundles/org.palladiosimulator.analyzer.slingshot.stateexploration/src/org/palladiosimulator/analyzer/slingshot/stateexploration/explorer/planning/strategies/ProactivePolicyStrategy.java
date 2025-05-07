package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies;

import java.util.List;

import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.StateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ExploredState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.FringeFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.PlannedTransition;

import com.google.common.base.Preconditions;

/**
 *
 * @author Sophie Stieß
 *
 */
public abstract class ProactivePolicyStrategy {

	protected final StateGraph graph;
	protected final FringeFringe fringe;

	/**
	 * Create new {@link ProactivePolicyStrategy}.
	 *
	 *
	 * @param graph  graph of the exploration, must not be {@code null}.
	 * @param fringe fringe of the exploration, must not be {@code null}.
	 */
	protected ProactivePolicyStrategy(final StateGraph graph, final FringeFringe fringe) {
		this.graph = Preconditions.checkNotNull(graph);
		this.fringe = Preconditions.checkNotNull(fringe);
	}

	/**
	 *
	 * Create proactive reconfigurations based on the given {@link ExploredState}.
	 *
	 * Ensures, that all of the {@link PlannedTransition}s in the resulting list are yet
	 * unexplored. I.e. neither is any of them is in the state graphs fringe, nor
	 * has any of them already been explored.
	 *
	 * @return
	 */
	public abstract List<PlannedTransition> createProactiveChanges();

}
