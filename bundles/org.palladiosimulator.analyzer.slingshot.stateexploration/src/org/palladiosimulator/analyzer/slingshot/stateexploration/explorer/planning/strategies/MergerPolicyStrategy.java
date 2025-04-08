package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ProactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.FringeFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.PlannedTransition;

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

	/** 
	 * max occurrence of policy per transition. 1 means, each policy at max once.  
	 */
	private static final int MAX_OCCURRENCE_PER_POLICY = 1;
	
	/** 
	 * max set (list) size  
	 */
	private static final int MAX_LIST_SIZE = 2; // TODO

	private final DefaultState state;

	/**
	 * Create new {@link MergerPolicyStrategy}.
	 *
	 * @param graph  graph of the exploration, must not be {@code null}.
	 * @param fringe fringe of the exploration, must not be {@code null}.
	 */
	protected MergerPolicyStrategy(final DefaultGraph graph, final FringeFringe fringe,
			final DefaultState state) {
		super(graph, fringe);
		this.state = state;
	}

	/**
	 * TODO
	 */
	@Override
	public List<PlannedTransition> createProactiveChanges() {

		if (state.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()
				|| state.getIncomingTransition().isEmpty()) {
			return List.of();
		}

		final List<ModelAdjustmentRequested> reactiveAdjustments = state.getSnapshot()
				.getModelAdjustmentRequestedEvent();
		final DefaultState predecessor = (DefaultState) state.getIncomingTransition().get().getSource();

		final Set<Reconfiguration> collectedReconfs = collectReconfigurationsAt(predecessor);

		final List<PlannedTransition> newTodos = new ArrayList<>();

		for (final Reconfiguration reconf : collectedReconfs) {

			for (Reconfiguration newReconf : createPermutations(reconf.getReactiveReconfigurationEvents(),
					reactiveAdjustments)) {
				if (!this.contains(collectedReconfs, newReconf)) {
					final PlannedTransition todoChange = new PlannedTransition(Optional.of(newReconf), predecessor);
					newTodos.add(todoChange);
				} else {
					LOGGER.debug(
							String.format("no new change based on change %s, because such a change already exists.",
									reconf.toString()));
				}
			}
		}

		return newTodos;
	}

	/**
	 * Create new {@link ProactiveReconfiguration}s base on the new and old ones.
	 * 
	 * Both {@code new} and {@code old} may be lists. Each element of {@code new} is
	 * individually added at each position in {@code old}. Elements form {@code new}
	 * are handled individually, because a list size greater than 1 is very
	 * unlikely, yet very complicated and thus not worth the (implementation)
	 * effort.
	 * 
	 * Ensures that the order of each list is retained.
	 * 
	 * Example: old=[a1,a2], new=[b1,b2]
	 * 
	 * Permutations: [b1,a1,a2],[a1,b1,a2],[a1,a2,b1],[b2,a1,a2],[a1,b2,a2],[a1,a2,b2]
	 *
	 * @param oldAdj adjustments already applied to or planned for the predecessor
	 *               state.
	 * @param newAdj reactive adjustments that occurred at a newer state.
	 * @return
	 */
	private Set<ProactiveReconfiguration> createPermutations(List<ModelAdjustmentRequested> oldAdj,
			List<ModelAdjustmentRequested> newAdj) {
		Set<ProactiveReconfiguration> proactiveReconfs = new HashSet<>();

		for (ModelAdjustmentRequested adj : newAdj) {
			if (countAdjustment(oldAdj, adj) < MAX_OCCURRENCE_PER_POLICY &&
					oldAdj.size() < MAX_LIST_SIZE) {
				for (int i = 0; i <= oldAdj.size(); i++) {
					List<ModelAdjustmentRequested> tmp = new ArrayList<>(oldAdj);
					tmp.add(i, adj);
					proactiveReconfs.add(new ProactiveReconfiguration(tmp));
				}
			} else {
				LOGGER.debug(String.format(
						"no new change based on policy %s because it already occurs more than %d times in the base change. Or because the resulting change list will be to big.",
						adj.getScalingPolicy().getEntityName(), MAX_OCCURRENCE_PER_POLICY));
			}
		}
		return proactiveReconfs;
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
				.map(e -> e.getScalingPolicy().getId()).sorted().reduce("", (a, b) -> a + b);

		for (final Reconfiguration change : changes) {

			final String otherIds = change.getReactiveReconfigurationEvents().stream()
					.map(e -> e.getScalingPolicy().getId()).sorted().reduce("", (a, b) -> a + b);

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
	 * @return nmber of occurences of reconf in change.
	 */
	private long countAdjustment(final List<ModelAdjustmentRequested> change, final ModelAdjustmentRequested reconf) {
		String policyId = reconf.getScalingPolicy().getId();
		long numberOfOccurence = change.stream().map(e -> e.getScalingPolicy().getId())
				.filter(id -> id.equals(policyId)).count();
		return numberOfOccurence;
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
	private Set<Reconfiguration> collectReconfigurationsAt(final DefaultState predecessor) {
		final Set<Reconfiguration> collectedChanges = new HashSet<>();

		final Set<Reconfiguration> exploredChanges = predecessor.getOutgoingTransitions().stream()
				.filter(transition -> transition.getChange().isPresent())
				.map(transition -> transition.getChange().get()).filter(Reconfiguration.class::isInstance)
				.map(Reconfiguration.class::cast).collect(Collectors.toSet());
		final Set<Reconfiguration> plannedChanges = fringe.getPlannedReconfFor(predecessor);

		collectedChanges.addAll(plannedChanges);
		collectedChanges.addAll(exploredChanges);

		return collectedChanges;
	}

}
