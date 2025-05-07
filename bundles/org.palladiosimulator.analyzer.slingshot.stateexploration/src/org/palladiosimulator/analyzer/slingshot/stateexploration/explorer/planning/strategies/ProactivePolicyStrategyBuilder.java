package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies;

import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.StateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ExploredState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.FringeFringe;

import com.google.common.base.Preconditions;

public class ProactivePolicyStrategyBuilder {

	protected final StateGraph graph;
	protected final FringeFringe fringe;

	public ProactivePolicyStrategyBuilder(final StateGraph graph, final FringeFringe fringe) {
		this.graph = Preconditions.checkNotNull(graph);
		this.fringe = Preconditions.checkNotNull(fringe);
	}

	public ProactivePolicyStrategy createBacktrackMergerPolicyStrategy(final ExploredState state) {
		return new MergerPolicyStrategy(graph, fringe, state);
	}

	public ProactivePolicyStrategy createBacktrackPolicyStrategy(final ExploredState state) {
		return new BacktrackPolicyStrategy(graph, fringe, state);
	}

}
