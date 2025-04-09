package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Set;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemorySnapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.common.base.Preconditions;

/**
 * Default implementation of the RawStateGraph.
 *
 * @author Sarah Stieß
 *
 */
public class DefaultGraph extends SimpleDirectedWeightedGraph<RawModelState, RawTransition> implements RawStateGraph {

	private static final long serialVersionUID = -7468814179743463536L;

	/** root state of the graph. Has no incoming transitions. */
	private final DefaultState root;
	
	/** state with the latest start time in the graph */
	private DefaultState furthestState;
	
	/**
	 * Create a new graph instance. 
	 * 
	 * The new graph contains a root state representing the given architecture configuration, but nothing else.  
	 *
	 * @param rootArchConfig architecture configuration for the root node.
	 */
	public DefaultGraph(final ArchitectureConfiguration rootArchConfig) {
		super(RawTransition.class);
		
		this.root = new DefaultState(0, rootArchConfig, this, null, new InMemorySnapshot(Set.of()), 0, Set.of(), Set.of());
		this.furthestState = this.root;
		
		this.addVertex(root);		
	}
	
	@Override
	public DefaultState getRoot() {
		return this.root;
	}

	@Override
	public Set<RawModelState> getStates() {
		return Set.copyOf(this.vertexSet());
	}

	@Override
	public Set<RawTransition> getTransitions() {
		return Set.copyOf(this.edgeSet());
	}
	
	/**
	 * Get the state with the latest start time.
	 * 
	 * @return state with the latest start time
	 */
	public DefaultState getFurthestState() {
		return furthestState;
	}

	/**
	 * Create an new state from the given builder and insert it into this graph instance.
	 * 
	 * Also creates and inserts the transition to connect the new state to the rest of the graph. 
	 *
	 * @param builder builder to create the new state and transition. 
	 * @return the newly created state.
	 */
	public DefaultState createAndInsertState(final DefaultStateBuilder builder) {
		final DefaultState newState = builder.buildState();
		this.addVertex(newState);
		
		final DefaultTransition newTransition = builder.buildTransition();
		
		this.addEdge(builder.getPPInfo().predecessor(), newState, newTransition);	
		this.updateFurthestState(newState);
		
		return newState;
	}
	
	/**
	 * Update the furthest state to the given state, if the start time of the given state is greater.
	 * 
	 * No changes, if the current furthest state is still the furthest.
	 * 
	 * @param state 
	 */
	private void updateFurthestState(final DefaultState state) {
		if (state.getStartTime() > this.furthestState.getStartTime()) {
			this.furthestState = state;
		}
	}

	/**
	 * Check whether the given model state has an outgoing transition that applies the given scaling policy.
	 * 
	 * The given state must be part of this graph.
	 *
	 * @param vertex
	 * @param matchee
	 * @return true, iff {@code vertex} has an outgoing transition that applies the given scaling policy.
	 */
	public boolean hasOutTransitionFor(final RawModelState vertex, final ScalingPolicy matchee) {		
		Preconditions.checkArgument(this.vertexSet().contains(vertex), String.format("State %s is not in the graph, but must be.", vertex.toString()));
		
		return this.outgoingEdgesOf(vertex).stream()
				.filter(t ->isOutTransitionFor(t, matchee))
				.findAny()
				.isPresent();
	}

	/**
	 * Check whether the given transition matches the given policy. 
	 * 
	 * (Intended as helper for {@link DefaultGraph#hasOutTransitionFor(RawModelState, ScalingPolicy)} only.) 
	 * 
	 * @param transition transition
	 * @param matchee policy to match
	 * @return true, iff {@code transition} matches {@code matchee}.
	 */
	private boolean isOutTransitionFor(final RawTransition transition, final ScalingPolicy matchee) {	
		return transition.getChange().isPresent()
		&& transition.getChange().get() instanceof Reconfiguration
		&& this.isReconfigurationFor((Reconfiguration) transition.getChange().get(), matchee);
	}
	
	/**
	 * Check whether the given reconfiguration matches the given policy.  
	 *
	 * (Intended as helper for {@link DefaultGraph#isOutTransitionFor(RawTransition, ScalingPolicy)} only.)
	 *
	 * @param reconf reconfiguration 
	 * @param matchee policy to match
	 * @return true, iff {@code reconf} matches {@code matchee}. 
	 */
	private boolean isReconfigurationFor(final Reconfiguration reconf, final ScalingPolicy matchee) {
		return reconf.getAppliedPolicies().size() == 1 && reconf.getAppliedPolicies().stream().map(p -> p.getId())
				.filter(id -> id.equals(matchee.getId())).count() == 1;
	}
	

	/**
	 * Calculate the distance between the given state and the given predecessor.
	 *
	 * The distance is the number of transitions in between the given states.
	 * 
	 * @param state       any state.
	 * @param predecessor a predecessor of the other state.
	 * @return the positive distance between the state, or 0 if they are the same.
	 * 
	 * @throws IllegalArgumentException if the state given as predecessor does not precede the other state.
	 */
	public static int distance(final RawModelState state, final RawModelState predecessor) {
		RawModelState current = state;
		int distance = 0;

		while (!current.equals(predecessor)) {
			if (current.getIncomingTransition().isEmpty()) {
				throw new IllegalArgumentException(String.format("State %s is not a predecessor of state %s.",
						predecessor.toString(), state.toString()));
			}
			current = current.getIncomingTransition().get().getSource();
			distance++;
		}

		return distance;
	}
}
