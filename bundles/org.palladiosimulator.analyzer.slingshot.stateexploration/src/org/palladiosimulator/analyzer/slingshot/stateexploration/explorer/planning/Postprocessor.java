package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ReactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies.ProactivePolicyStrategy;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies.ProactivePolicyStrategyBuilder;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraphFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
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

	private final DefaultGraph rawgraph;
	private final DefaultGraphFringe fringe;

	private final ProactivePolicyStrategyBuilder proactiveStrategyBuilder;

	/**
	 * 
	 * @param graph
	 * @param fringe
	 */
	public Postprocessor(final DefaultGraph graph, final DefaultGraphFringe fringe) {
		this.rawgraph = graph;
		this.fringe = fringe;

		this.proactiveStrategyBuilder = new ProactivePolicyStrategyBuilder(this.rawgraph, this.fringe);

		this.updateGraphFringe(graph.getRoot());
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
		
		// NOP Always
		this.fringe.add(new PlannedTransition(Optional.empty(), start));

		// Reactive Reconfiguration - Always.
		if (start.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()) {
			return;
		}
		// TODO : this is the "each once" implementation, but i'm not sure whether it's
		// the best.
		for (final ModelAdjustmentRequested event : start.getSnapshot().getModelAdjustmentRequestedEvent()) {
			// reactive reconf to the next state.
			this.fringe.add(new PlannedTransition(Optional.of(new ReactiveReconfiguration(event)), start));
		}

		// proactive reconfs
		final List<PlannedTransition> newTransitions = new ArrayList<>();

		newTransitions
				.addAll(this.proactiveStrategyBuilder.createBacktrackPolicyStrategy(start).createProactiveChanges());
		newTransitions.addAll(
				this.proactiveStrategyBuilder.createBacktrackMergerPolicyStrategy(start).createProactiveChanges());

		// only add net yet executed or queued changes.
		for (final PlannedTransition toDoChange : newTransitions) {

			Set<Transition> transitions = new HashSet<>();
			transitions.addAll(this.rawgraph.getTransitions());
			transitions.addAll(this.fringe.getAllPlannedTransition());

			Boolean dup = transitions.stream().map(toDoChange::isSame).reduce(false, (b1, b2) -> b1 || b2);

			if (!dup) {
				this.fringe.add(toDoChange);
			}
		}
	}
}
