package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;

/**
 * Default implementation of the RawStateGraph.
 *
 *
 * @author stiesssh
 *
 */
public class DefaultGraph implements RawStateGraph{

	private final Set<RawModelState> nodes;
	private final Set<RawTransition> transitions;

	private final ArrayDeque<ToDoChange> fringe;

	private final DefaultState root;

	public DefaultGraph(final DefaultState root) {
		super();
		this.nodes = new HashSet<RawModelState>();
		this.transitions = new HashSet<RawTransition>();
		this.fringe = new ArrayDeque<ToDoChange>();
		this.root = root;

		this.nodes.add(root);
	}

	/**
	 * Select the change for the next exploration cycle.
	 *
	 * Current strategy is FIFO, but should be imroved to something more intelligent later on.
	 *
	 * @return
	 */
	public ToDoChange getNext() {
		final ToDoChange todochange = this.fringe.poll();
		//((BloatedState) edge.getSource()).addOutTransition(edge);
		return todochange;
	}

	/**
	 *
	 * @return true, iff there's another change to explore, false otherwise
	 */
	public Boolean hasNext() {
		return !this.fringe.isEmpty();
	}

	public void addNode(final RawModelState node) {
		this.nodes.add(node);
	}

	/**
	 * The fringe are the Changes to be
	 * @param edge
	 */
	public void addFringeEdge(final ToDoChange edge) {
		fringe.add(edge);
	}

	@Override
	public DefaultState getRoot() {
		return this.root;
	}

	@Override
	public Set<RawModelState> getStates() {
		return this.nodes;
	}

	@Override
	public Set<RawTransition> getTransitions() {
		return this.nodes.stream().map(state -> state.getOutTransitions()).reduce(new HashSet<>(), (s,t) -> {s.addAll(t); return s;});
	}
}
