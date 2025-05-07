package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;


/**
 * 
 * @author Sophie Stieß
 *
 */
public class ExploredTransition implements Transition {
	private final Optional<Change> change;

	private final StateGraph graph;

	/**
	 * Create a new transition.
	 * 
	 * @param change
	 * @param graph
	 */
	protected ExploredTransition(final Optional<Change> change, final StateGraph graph) {
		super();
		this.change = change;
		this.graph = graph;
	}

	@Override
	public ExploredState getSource() {
		return this.graph.getEdgeSource(this);
	}

	public ExploredState getTarget() {
		return this.graph.getEdgeTarget(this);
	}

	@Override
	public Optional<Change> getChange() {
		return this.change;
	}
	
	@Override
	public String toString() {
		if (change.isEmpty()) {
			return "NOP for " + getSource().getId();
		} else {
			return String.format("%s for %s", change.get().toString(), getSource().getId());
		}
	}
	
	public double getPointInTime() {
		return this.getSource().getEndTime();
	}

	public String getName() {
		return String.format("%s -> %s", getSource().getId(),getTarget().getId());
	}
}
