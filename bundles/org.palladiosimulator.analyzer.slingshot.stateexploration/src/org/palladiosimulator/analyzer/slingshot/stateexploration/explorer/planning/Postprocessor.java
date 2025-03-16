package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ReactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies.ProactivePolicyStrategy;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies.ProactivePolicyStrategyBuilder;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.FringeFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.PlannedTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.Transition;

/**
 *
 * Post process a node of the state graph after the node was simulated.
 * 
 * Postprocessing includes adding new planned transitions to the fringe. Planned
 * transitions include at least NOOP-transition. If the state ended with a
 * {@link ModelAdjustmentRequested} event, postprocessing also creates a planned
 * transition for a reactive reconfiguration, and multiple planned transitions
 * for proactive reconfigurations.
 * 
 * Proactive reconfigurations are added according to different strategies, c.f.
 * {@link ProactivePolicyStrategy}.
 * 
 * 
 * @author Sophie Stieß
 *
 */
public class Postprocessor {

	private static final Logger LOGGER = Logger.getLogger(Postprocessor.class.getName());

	private final int keepAtLeast;
	
	private final DefaultGraph rawgraph;
	private final FringeFringe fringe;

	private final ProactivePolicyStrategyBuilder proactiveStrategyBuilder;

	/**
	 * 
	 * @param graph
	 * @param fringe
	 */
	public Postprocessor(final DefaultGraph graph, final FringeFringe fringe) {
		this.rawgraph = graph;
		this.fringe = fringe;

		this.proactiveStrategyBuilder = new ProactivePolicyStrategyBuilder(this.rawgraph, this.fringe);

		this.updateGraphFringe(graph.getRoot());
		
		this.keepAtLeast = graph.getRoot().getArchitecureConfiguration().getSPD().isEmpty() ? 1 : graph.getRoot().getArchitecureConfiguration().getSPD().get().getScalingPolicies().size();
	}

	/**
	 * Add new exploration directions to the graph fringe.
	 *
	 * To be called after the given state was simulated.
	 *
	 * This is where the proactive / reactive / nop changes for future iterations
	 * are set. If we want to add more proactive changes later on, this operation is
	 * where we should insert them.
	 *
	 * @param start state that we just finished exploring.
	 */
	public void updateGraphFringe(final DefaultState start) {
		
		if (start.getReasonsToLeave().contains(ReasonToLeave.aborted)) {
			return;
		}
			
		final Set<RawModelState> worstSiblings = this.getWorstSiblingSuccesors(start);
		this.fringe.prune(pt -> worstSiblings.contains(pt.getSource()));
		
		if (worstSiblings.contains(start)) {
			return;
		}
		
		// NOP Always
		this.fringe.offer(new PlannedTransition(Optional.empty(), start));

		// Reactive Reconfiguration - Always.
		if (start.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()) {
			return;
		}
		// TODO : this is the "each once" implementation, but i'm not sure whether it's
		// the best.
		for (final ModelAdjustmentRequested event : start.getSnapshot().getModelAdjustmentRequestedEvent()) {
			// reactive reconf to the next state.
			this.fringe.offer(new PlannedTransition(Optional.of(new ReactiveReconfiguration(event)), start));
		}

		// proactive reconfs
		final List<PlannedTransition> newTransitions = new ArrayList<>();

		newTransitions
				.addAll(this.proactiveStrategyBuilder.createBacktrackPolicyStrategy(start).createProactiveChanges());
		newTransitions.addAll(
				this.proactiveStrategyBuilder.createBacktrackMergerPolicyStrategy(start).createProactiveChanges());

		// only add net yet executed or queued changes.
		for (final PlannedTransition toDoChange : newTransitions) {

			final Set<Transition> transitions = new HashSet<>();
			transitions.addAll(this.rawgraph.getTransitions());
			transitions.addAll(this.fringe.getAllPlannedTransition());

			final Boolean dup = transitions.stream().map(toDoChange::isSame).reduce(false, (b1, b2) -> b1 || b2);

			if (!dup) {
				this.fringe.offer(toDoChange);
			}
		}
	}
	
	/**
	 * 
	 * Cut off all but the best few siblings of the given state.
	 * 
	 * Cut off means 
	 * 
	 * @param state 
	 */
	public Set<RawModelState> getWorstSiblingSuccesors(final DefaultState state) {
		if (state.getIncomingTransition().isEmpty()) {
			return Set.of(); // root
		}
		final DefaultState parent = (DefaultState) state.getIncomingTransition().get().getSource();
		final List<DefaultState> siblings = parent.getOutgoingTransitions().stream().map(t -> (DefaultState) t.getTarget()).toList();
		
		if (siblings.size() <= keepAtLeast) {
			return Set.of(); //too few states
		}
		
		final int limit = siblings.size() - keepAtLeast;

		final List<DefaultState> worstSiblings = siblings.stream().sorted((s1,s2) -> Double.compare(s1.getUtility(), s2.getUtility())).limit(limit).toList();

		final Set<RawModelState> badSuccessors = new HashSet<>();
		for (final DefaultState badState : worstSiblings) {
			badSuccessors.add(badState);
			badSuccessors.addAll(this.collectSuccessors(badState));
		}	
		return badSuccessors;
	}
	
	
	/**
	 * 
	 * Collect all successors of a given state.
	 * 
	 * @param state
	 * @return successor states of {@code} state
	 */
	public Set<RawModelState> collectSuccessors(final RawModelState state) {		
		final Set<RawModelState> successors = new HashSet<>();
		
		for (final RawTransition transition : state.getOutgoingTransitions()) {
			successors.addAll(this.collectSuccessors(transition.getTarget()));
		}
		return successors;	
	}
}
