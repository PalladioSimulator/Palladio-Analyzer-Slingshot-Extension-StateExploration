package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ProactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraphFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.PlannedTransition;
import org.palladiosimulator.spd.ScalingPolicy;

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

	private static final Logger LOGGER = Logger.getLogger(MergerPolicyStrategy.class.getName());

	private static final int DUPLICATE_THRESHOLD = 2;
	
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

	/**
	 * TODO 
	 */
	@Override
	public List<PlannedTransition> createProactiveChanges() {

		if (state.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty() || state.getIncomingTransition().isEmpty() ) {
			return List.of();
		}

		final List<ModelAdjustmentRequested> reconfigurationToBeApplied = state.getSnapshot()
				.getModelAdjustmentRequestedEvent();
		final DefaultState predecessor = (DefaultState) state.getIncomingTransition().get().getSource();

		final Set<Reconfiguration> collectedChanges = collectChanges(predecessor);

		final List<PlannedTransition> newTodos = new ArrayList<>();

		for (final Change change : collectedChanges) {
			
			for (ModelAdjustmentRequested adjustment : reconfigurationToBeApplied) {
				if (this.tooManyOccurences((Reconfiguration) change, adjustment)) {
					LOGGER.debug(String.format(
							"no new change based on change %s, because policy %s already occurs more than %d times in that change.",
							change.toString(), adjustment.getScalingPolicy().getEntityName(), DUPLICATE_THRESHOLD));
					continue;
				}
			}

			final List<ModelAdjustmentRequested> adjustments = new ArrayList<>(((Reconfiguration) change)
					.getReactiveReconfigurationEvents());
			adjustments.addAll(reconfigurationToBeApplied);


			final Reconfiguration newChange = new ProactiveReconfiguration(adjustments);

			if (!this.contains(collectedChanges, newChange)) {
				final PlannedTransition todoChange = new PlannedTransition(Optional.of(newChange), predecessor);
				newTodos.add(todoChange);
			} else {
				LOGGER.debug(String.format("no new change based on change %s, because such a change already exists.",
						change.toString()));
			}
		}

		return newTodos;
	}

	/**
	 *
	 * Check whether a {@link Reconfiguration} with the same policies as
	 * {@code newChange} is already contained in {@code changes}.
	 *
	 * Beware, the check is executed base on the policy ids.
	 *
	 * Alternatively one could override the {@link Object#equals(Object)} operation
	 * of {@link Reconfiguration}, but i deemed it to risky.
	 *
	 * @param changes   the container
	 * @param newChange the element to check the container for.
	 * @return true iff {@code changes} contains {@code newChange}.
	 */
	private boolean contains(final Set<Reconfiguration> changes, final Reconfiguration newChange) {

		final String newChangeIds = newChange.getReactiveReconfigurationEvents().stream()
				.map(e -> e.getScalingPolicy().getId()).sorted().reduce("",
						(a, b) -> a + b);

		for (final Reconfiguration change : changes) {

			final String otherIds = change.getReactiveReconfigurationEvents().stream()
					.map(e -> e.getScalingPolicy().getId()).sorted().reduce("",
							(a, b) -> a + b);

			if (newChangeIds.equals(otherIds)) {
				return true;
			}
		}
		return false;

	}
	
	/**
	 * 
	 * @param change
	 * @param reconf
	 * @return true, iff reconf is already in change too often, i.e. shall not be added again. 
	 */
	private boolean tooManyOccurences(final Reconfiguration change, final ModelAdjustmentRequested reconf) {
		String policyId = reconf.getScalingPolicy().getId();
		long numberOfOccurence = change.getAppliedPolicies().stream().map(ScalingPolicy::getId).filter(id -> id.equals(policyId)).count();	
		return numberOfOccurence > DUPLICATE_THRESHOLD;
	}

	/**
	 * Collect already explored or planned {@link Reconfiguration}s.
	 *
	 * Only collects {@link Reconfiguration}s. NOPs or other types of changes are
	 * not included.
	 *
	 * @param predecessor state for which reconfigurations are collected.
	 * @return Set of already explored or planned {@link Reconfiguration}s
	 */
	private Set<Reconfiguration> collectChanges(final DefaultState predecessor) {
		final Set<Reconfiguration> collectedChanges = new HashSet<>();

		final Set<Reconfiguration> exploredChanges = predecessor.getOutgoingTransitions().stream()
				.filter(transition -> transition.getChange().isPresent())
				.map(transition -> transition.getChange().get())
				.filter(Reconfiguration.class::isInstance)
				.map(Reconfiguration.class::cast)
				.collect(Collectors.toSet());
		final Set<Reconfiguration> plannedChanges = fringe.getPlannedReconfFor(predecessor);

		collectedChanges.addAll(plannedChanges);
		collectedChanges.addAll(exploredChanges);

		return collectedChanges;
	}

}
