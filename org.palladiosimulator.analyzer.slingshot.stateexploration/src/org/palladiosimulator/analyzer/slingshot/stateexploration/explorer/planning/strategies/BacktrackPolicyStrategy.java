package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
 * reconfiguration. Always creates at most one proactive Change for each
 * reactive change.
 *
 * Takes the reactively applied reconfiguration, backtracks through the state
 * graph until it finds a state, where the reconfiguration was not yet applied
 * and created the {@link ToDoChange} to apply it.
 *
 * @author Sarah Stieß
 *
 */
public class BacktrackPolicyStrategy extends ProactivePolicyStrategy {

	private final DefaultGraph graph;
	private final DefaultGraphFringe fringe;

	public BacktrackPolicyStrategy(final DefaultGraph graph, final DefaultGraphFringe fringe) {
		this.graph = graph;
		this.fringe = fringe;
	}

	@Override
	public Collection<ToDoChange> createProactiveChanges(final DefaultState state) {

		if (state.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()) {
			return List.of();
		}

		final Set<ModelAdjustmentRequested> events = state.getSnapshot().getModelAdjustmentRequestedEvent();

		final Collection<ToDoChange> rval = new ArrayList<>();

		for (final ModelAdjustmentRequested event : events) {
			rval.addAll(createProactiveChange(state, event));
		}

		return rval;
	}

	/**
	 *
	 * Create a proactive reconfiguration from the given event at a predecessor of
	 * {@code state}.
	 *
	 * Selects the predecessor closest to {@code state} where the reconfiguration of
	 * the given event was not yet applied. If no such predecessor exists, the
	 * returned collection is empty.
	 *
	 * @param state create a proactive reconfiguration before this state
	 * @param event the reconfiguration
	 * @return A set with the proactive change, of an empty set if no fitting
	 *         predecessor exists.
	 */
	private Collection<ToDoChange> createProactiveChange(final DefaultState state,
			final ModelAdjustmentRequested event) {
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
