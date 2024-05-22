package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Optional;
import java.util.Set;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemorySnapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
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

	private final DefaultState root;

	/**
	 *
	 *
	 * @param rootArchConfig architecture configuration for the root node.
	 */
	public DefaultGraph(final ArchitectureConfiguration rootArchConfig) {
		super(RawTransition.class);

		this.root = this.insertStateFor(0.0, rootArchConfig);
		this.root.setSnapshot(new InMemorySnapshot(Set.of()));
	}



	public DefaultTransition insertTransitionFor(final Optional<Change> change, final RawModelState source,
			final RawModelState target) {
		final DefaultTransition newTransition = new DefaultTransition(change, this);
		this.addEdge(source, target, newTransition);
		return newTransition;

	}

	/**
	 *
	 * @param startPointInTime
	 * @param archConfig
	 * @return
	 */
	public DefaultState insertStateFor(final double startPointInTime, final ArchitectureConfiguration archConfig) {
		final DefaultState newState = new DefaultState(startPointInTime, archConfig, this);
		this.addVertex(newState);
		return newState;
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
		return Set.copyOf(this.vertexSet());
	}

	@Override
	public Set<RawTransition> getTransitions() {
		return Set.copyOf(this.edgeSet());
	}
}
