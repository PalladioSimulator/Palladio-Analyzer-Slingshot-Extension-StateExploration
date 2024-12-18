package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies;

import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraphFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;

import com.google.common.base.Preconditions;

public class ProactivePolicyStrategyBuilder {

	protected final DefaultGraph graph;
	protected final DefaultGraphFringe fringe;

	public ProactivePolicyStrategyBuilder(final DefaultGraph graph, final DefaultGraphFringe fringe) {
		this.graph = Preconditions.checkNotNull(graph);
		this.fringe = Preconditions.checkNotNull(fringe);
	}

	public ProactivePolicyStrategy createBacktrackMergerPolicyStrategy(final DefaultState state) {
		return new MergerPolicyStrategy(graph, fringe, state);
	}

	public ProactivePolicyStrategy createBacktrackPolicyStrategy(final DefaultState state) {
		return new BacktrackPolicyStrategy(graph, fringe, state);
	}

}
