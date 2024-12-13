package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies;

import java.util.List;
import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ProactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraphFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ToDoChange;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * Creates new {@code ToDoChange}s by backtracking after an reactive
 * reconfiguration. Always creates at most one proactive Change.
 *
 * Takes the reactively applied reconfiguration, backtracks through the state
 * graph until it finds a state, where the reconfiguration was not yet applied
 * and created the {@link ToDoChange} to apply it.
 *
 * @author stiesssh
 *
 */
public class BacktrackPolicyStrategy extends ProactivePolicyStrategy {

	private final DefaultState state;

	/**
	 * Create new {@link BacktrackPolicyStrategy}.
	 *
	 *
	 * @param graph  graph of the exploration, must not be {@code null}.
	 * @param fringe fringe of the exploration, must not be {@code null}.
	 */
	protected BacktrackPolicyStrategy(final DefaultGraph graph, final DefaultGraphFringe fringe, final DefaultState state) {
		super(graph, fringe);
		this.state = state;
	}

	@Override
	public List<ToDoChange> createProactiveChanges() {

		if (state.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()) {
			return List.of();
		}

		final ModelAdjustmentRequested event = state.getSnapshot().getModelAdjustmentRequestedEvent().get();

		DefaultState predecessor = state;

		if (!this.graph.getRoot().equals(state)) {
			predecessor = getPredecessor(state);
		}

		while (policyAlreadyExploredAtState(predecessor, event.getScalingPolicy())) {
			if (this.graph.getRoot().equals(predecessor)) {
				return List.of();
			}
			predecessor = getPredecessor(predecessor);
		}

		return List.of(new ToDoChange(Optional.of(new ProactiveReconfiguration(event)), predecessor));
	}

	/**
	 * Check whether {@code state} already transitioned to a successor, via the
	 * given policy. Or whether it is already planned to explore that (i.e.
	 * corresponding change in the fringe)
	 *
	 * @param state
	 * @param policy
	 * @return
	 */
	private boolean policyAlreadyExploredAtState(final DefaultState state, final ScalingPolicy policy) {
		return this.graph.hasOutTransitionFor(state, policy) || this.fringe.containsTodoFor(state, policy);
	}

	/**
	 * Get predecessor of the given state.
	 *
	 * Requires that {@code state} is not root.
	 *
	 * @param state
	 * @return
	 */
	private DefaultState getPredecessor(final DefaultState state) {
		assert !this.graph.getRoot().equals(state);

		return (DefaultState) graph.getTransitions().stream()
				.filter(t -> t.getTarget().equals(state))
				.map(t -> t.getSource())
				.findFirst().get();
	}
}
