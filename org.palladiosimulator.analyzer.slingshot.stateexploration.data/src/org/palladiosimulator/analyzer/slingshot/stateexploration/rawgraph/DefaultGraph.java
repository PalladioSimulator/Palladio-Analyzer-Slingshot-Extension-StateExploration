package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.ArrayDeque;
import java.util.Set;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 * Default implementation of the RawStateGraph.
 *
 *
 * @author stiesssh
 *
 */
public class DefaultGraph extends SimpleDirectedWeightedGraph<RawModelState, RawTransition> implements RawStateGraph {

	/**
	 *
	 */
	private static final long serialVersionUID = -7468814179743463536L;

	private final ArrayDeque<ToDoChange> fringe;

	private final DefaultState root;

	public DefaultGraph(final DefaultState root) {
		super(RawTransition.class);
		this.addVertex(root);

		this.fringe = new ArrayDeque<ToDoChange>();
		this.root = root;
	}

	/**
	 * Select the change for the next exploration cycle.
	 *
	 * Current strategy is FIFO, but should be imroved to something more intelligent later on.
	 *
	 * @return
	 */
	public ToDoChange getNext() {
		return this.fringe.poll();
	}

	/**
	 *
	 * @return true, iff there's another change to explore, false otherwise
	 */
	public Boolean hasNext() {
		return !this.fringe.isEmpty();
	}

	/**
	 * The fringe are the Changes to be
	 * @param edge
	 */
	public void addFringeEdge(final ToDoChange edge) {
		fringe.add(edge);
	}

	public boolean hasInFringe(final DefaultState state, final ScalingPolicy matchee) {
		return this.fringe.stream()
				.filter(todo -> todo.getStart().equals(state)
						&& todo.getChange().isPresent()
						&& todo.getChange().get() instanceof Reconfiguration
						&& ((Reconfiguration) todo.getChange().get()).getAppliedPolicy().getId()
								.equals(matchee.getId()))
				.findAny()
				.isPresent();

	}

	public boolean hasOutTransitionFor(final RawModelState vertex, final ScalingPolicy matchee) {
		return this.outgoingEdgesOf(vertex).stream()
				.filter(t -> t.getChange().isPresent()
						&& t.getChange().get() instanceof Reconfiguration
						&& ((Reconfiguration) t.getChange().get()).getAppliedPolicy().getId().equals(matchee.getId()))
				.findAny()
				.isPresent();
	}

	@Override
	public DefaultState getRoot() {
		return this.root;
	}

	@Override
	public Set<RawModelState> getStates() {
		return this.vertexSet();
	}

	@Override
	public Set<RawTransition> getTransitions() {
		return this.edgeSet();
	}
}
