package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ProactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.fringe.FringeFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.ExploredState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.PlannedTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.StateGraph;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * Creates new {@code ToDoChange}s by backtracking after an reactive
 * reconfiguration. Always creates at most one proactive Change.
 *
 * Takes the reactively applied reconfiguration, backtracks through the state
 * graph until it finds a state, where the reconfiguration was not yet applied
 * and created the {@link PlannedTransition} to apply it.
 *
 * @author Sophie Stieß
 *
 */
public class BacktrackPolicyStrategy extends ProactivePolicyStrategy {

	private final ExploredState state;
	
	private static final int MAX_DISTANCE = 3;

	/**
	 * Create new {@link BacktrackPolicyStrategy}.
	 *
	 *
	 * @param graph  graph of the exploration, must not be {@code null}.
	 * @param fringe fringe of the exploration, must not be {@code null}.
	 */
	protected BacktrackPolicyStrategy(final StateGraph graph, final FringeFringe fringe, final ExploredState state) {
		super(graph, fringe);
		this.state = state;
	}

	@Override
	public List<PlannedTransition> createProactiveChanges() {

		if (state.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()) {
			return List.of();
		}

		final List<ModelAdjustmentRequested> events = state.getSnapshot().getModelAdjustmentRequestedEvent();

		final List<PlannedTransition> rval = new ArrayList<>();

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
	private List<PlannedTransition> createProactiveChange(final ExploredState state,
			final ModelAdjustmentRequested event) {
		ExploredState predecessor = state;

		if (!this.graph.getRoot().equals(state)) {
			predecessor = getPredecessor(state);
		}

		int distance = 0; 
		while (distance < MAX_DISTANCE && policyAlreadyExploredAtState(predecessor, event.getScalingPolicy())) {
			if (this.graph.getRoot().equals(predecessor)) {
				return List.of();
			}
			predecessor = getPredecessor(predecessor);
			distance++;
		}

		return List.of(new PlannedTransition(Optional.of(new ProactiveReconfiguration(event)), predecessor));
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
	private boolean policyAlreadyExploredAtState(final ExploredState state, final ScalingPolicy policy) {
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
	private ExploredState getPredecessor(final ExploredState state) {
		assert !this.graph.getRoot().equals(state);

		return (ExploredState) graph.getTransitions().stream()
				.filter(t -> t.getTarget().equals(state))
				.map(t -> t.getSource())
				.findFirst().get();
	}
}
