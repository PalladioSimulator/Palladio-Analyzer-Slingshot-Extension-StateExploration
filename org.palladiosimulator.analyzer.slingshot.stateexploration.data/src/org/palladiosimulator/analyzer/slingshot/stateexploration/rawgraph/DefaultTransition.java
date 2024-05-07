package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;

public class DefaultTransition implements RawTransition {
	private final Optional<Change> change;

	private final DefaultGraph graph;

	public DefaultTransition(final Optional<Change> change, final DefaultGraph graph) {
		super();
		this.change = change;
		this.graph = graph;
	}


	@Override
	public RawModelState getSource() {
		return this.graph.getEdgeSource(this);
	}

	@Override
	public RawModelState getTarget() {
		return this.graph.getEdgeTarget(this);
	}

	@Override
	public Optional<Change> getChange() {
		return this.change;
	}
}
