package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ProactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraphFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ToDoChange;

/**
 *
 * When a Policy happens reactively, it is always the first one. after all, it
 * just ended this state.
 *
 * Backtrack to the first predecessor where other (or the same) policies were
 * already applied, and create a new change that applies both.
 *
 * Unless the policies contradict each other (in/out on the same target group)
 *
 * This sucks for the espresso example, as there are only the two competing
 * policies.
 *
 * @author Sophie Stieß
 *
 */
public class MergerPolicyStrategy extends ProactivePolicyStrategy {

	private final DefaultState state;

	/**
	 * Create new {@link MergerPolicyStrategy}.
	 *
	 *
	 * @param graph  graph of the exploration, must not be {@code null}.
	 * @param fringe fringe of the exploration, must not be {@code null}.
	 */
	protected MergerPolicyStrategy(final DefaultGraph graph, final DefaultGraphFringe fringe, final DefaultState state) {
		super(graph, fringe);
		this.state = state;
	}

	@Override
	public List<ToDoChange> createProactiveChanges() {

		if (state.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty() || state.getIncomingTransition().isEmpty() ) {
			return List.of();
		}

		final ModelAdjustmentRequested reconfigurationToBeApplied = state.getSnapshot()
				.getModelAdjustmentRequestedEvent().get();
		final DefaultState predecessor = (DefaultState) state.getIncomingTransition().get().getSource();

		final Set<Change> collectedChanges = collectChanges(predecessor);

		final List<ToDoChange> newTodos = new ArrayList<>();

		for (final Change change : collectedChanges) {

			final Set<ModelAdjustmentRequested> adjustments = ((Reconfiguration) change)
					.getReactiveReconfigurationEvents();
			adjustments.add(reconfigurationToBeApplied);

			final Change newChange = new ProactiveReconfiguration(adjustments);
			final ToDoChange todoChange = new ToDoChange(Optional.of(newChange), predecessor);

			newTodos.add(todoChange);
		}


		return newTodos;
	}

	/**
	 * TODO
	 *
	 * @param predecessor
	 * @return
	 */
	private Set<Change> collectChanges(final DefaultState predecessor) {
		final Set<Change> collectedChanges = new HashSet<>();

		final Set<Change> exploredChanges = predecessor.getOutgoingTransitions().stream()
				.filter(transition -> transition.getChange().isPresent())
				.map(transition -> transition.getChange().get()).collect(Collectors.toSet());
		final Set<Change> plannedChanges = fringe.getPlannedReconfFor(predecessor);

		collectedChanges.addAll(plannedChanges);
		collectedChanges.addAll(exploredChanges);

		return collectedChanges;
	}

}
