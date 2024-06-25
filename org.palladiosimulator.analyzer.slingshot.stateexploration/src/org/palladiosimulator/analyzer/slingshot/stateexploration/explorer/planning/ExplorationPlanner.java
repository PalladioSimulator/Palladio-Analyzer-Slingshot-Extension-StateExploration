package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ReactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration.SimulationInitConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies.BacktrackPolicyStrategy;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.strategies.ProactivePolicyStrategy;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraphFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ToDoChange;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.triggers.BaseTrigger;
import org.palladiosimulator.spd.triggers.expectations.ExpectedTime;
import org.palladiosimulator.spd.triggers.stimuli.SimulationTime;

/**
 *
 * The ExplorationPlanner decides on the direction of the exploration.
 *
 * Handles fringe.
 *
 * @author Sarah Stieß
 *
 */
public class ExplorationPlanner {

	private static final Logger LOGGER = Logger.getLogger(ExplorationPlanner.class.getName());

	private final DefaultGraph rawgraph;
	private final DefaultGraphFringe fringe;
	private final CutOffConcerns cutOffConcerns;

	private final ProactivePolicyStrategy proactivePolicyStrategy;

	private final double minDuration;

	public ExplorationPlanner(final DefaultGraph graph, final DefaultGraphFringe fringe, final double minDuration) {
		this.rawgraph = graph;
		this.fringe = fringe;
		this.minDuration = minDuration;
		this.cutOffConcerns = new CutOffConcerns();

		this.proactivePolicyStrategy = new BacktrackPolicyStrategy(this.rawgraph, this.fringe);

		this.updateGraphFringePostSimulation(graph.getRoot());
	}

	/**
	 *
	 *
	 * @return Configuration for the next simulation run
	 */
	public SimulationInitConfiguration createConfigForNextSimualtionRun() {
		assert !this.fringe.isEmpty();

		ToDoChange next = this.fringe.poll();

		while (!this.cutOffConcerns.shouldExplore(next)) {
			LOGGER.debug(String.format("Future %s is bad, won't explore.", next.toString()));
			// TODO : Exception if the entire fringe is bad.
			next = this.fringe.poll();
		}

		final DefaultState start = next.getStart();
		final DefaultState end = this.createNewGraphNode(next);

		if (end.getArchitecureConfiguration().getSPD().isPresent()) {
			this.reduceSimulationTimeTriggerExpectedTime(end.getArchitecureConfiguration().getSPD().get(),
					start.getDuration());
		}

		return createConfigBasedOnChange(next.getChange(), start, end);

	}

	/**
	 *
	 *
	 * @param change
	 * @param start
	 * @param end
	 * @return
	 */
	private SimulationInitConfiguration createConfigBasedOnChange(final Optional<Change> change,
			final DefaultState start, final DefaultState end) {

		final double duration = this.calculateRunDuration(start);

		if (change.isEmpty()) {
			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, null, start.getId());
		}

		if (change.get() instanceof final Reconfiguration reconf) {
			LOGGER.debug("Create InitConfiguration for Reconfiguration (Pro- or Reactive)");


			final ModelAdjustmentRequested initEvent = (new AdjustorEventConcerns(end.getArchitecureConfiguration()))
					.copy(reconf.getReactiveReconfigurationEvent());

			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, initEvent, start.getId());

		}

		throw new UnsupportedOperationException("Environment Change not yet supported.");
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
	public void updateGraphFringePostSimulation(final DefaultState start) {
		// NOP Always
		this.fringe.add(new ToDoChange(Optional.empty(), start));
		// Reactive Reconfiguration - Always.
		if (start.getSnapshot().getModelAdjustmentRequestedEvent().isPresent()) {
			final ModelAdjustmentRequested event = start.getSnapshot().getModelAdjustmentRequestedEvent().get();

			// reactive reconf to the next state.
			this.fringe.add(new ToDoChange(Optional.of(new ReactiveReconfiguration(event)), start));
		}

		// proactive reconf.
		final List<ToDoChange> proactiveChanges = this.proactivePolicyStrategy.createProactiveChanges(start);

		for (final ToDoChange toDoChange : proactiveChanges) {
			this.fringe.add(toDoChange);
		}
	}




	/**
	 * Create a new graph note with a new arch configuration.
	 *
	 * creating a fully connected Graph node encompasses : - copying architecture
	 * configuration from preceding state. - setting the start time of the new node
	 * wrt. global time. - adding the node to the graph's node list. - creating
	 * transition to connect new node t predecessor.
	 *
	 * @return a new node, connected to its predecessor in graph.
	 */
	private DefaultState createNewGraphNode(final ToDoChange next) {
		final DefaultState predecessor = next.getStart();

		final ArchitectureConfiguration newConfig = predecessor.getArchitecureConfiguration().copy();
		final DefaultState newNode = this.rawgraph.insertStateFor(predecessor.getEndTime(), newConfig);

		this.rawgraph.insertTransitionFor(next.getChange(), predecessor, newNode);

		return newNode;
	}

	/**
	 * TODO : maybe try that thing with variable intervals (again).
	 *
	 * @param previous
	 * @return max duration for the next state
	 */
	private double calculateRunDuration(final DefaultState previous) {
		if (previous.getDuration() == 0 || previous.isDecreaseInterval()) {
			return minDuration;
		}
		return 2 * previous.getDuration();
	}

	/**
	 * Reduces the {@link ExpectedTime} value for scaling policies with trigger
	 * stimulus {@link SimulationTime} or deactivates the policy if the trigger is
	 * in the past with regard to global time.
	 *
	 * The {@link ExpectedTime} value is reduced by the duration of the previous
	 * state.
	 *
	 * @param spd    current scaling rules.
	 * @param offset duration of the previous state
	 */
	private void reduceSimulationTimeTriggerExpectedTime(final SPD spd, final double offset) {
		spd.getScalingPolicies().stream()
		.filter(policy -> policy.isActive() && policy.getScalingTrigger() instanceof BaseTrigger)
		.map(policy -> ((BaseTrigger) policy.getScalingTrigger()))
		.filter(trigger -> trigger.getStimulus() instanceof SimulationTime
				&& trigger.getExpectedValue() instanceof ExpectedTime)
		.forEach(trigger -> this.updateValue(((ExpectedTime) trigger.getExpectedValue()), offset));

		ResourceUtils.saveResource(spd.eResource());
	}

	/**
	 * update value helper.
	 *
	 * @param time                  model element to be updated
	 * @param previousStateDuration duration to subtract from {@code time}.
	 */
	private void updateValue(final ExpectedTime time, final double previousStateDuration) {
		final double triggerTime = time.getValue();

		final ScalingPolicy policy = (ScalingPolicy) time.eContainer().eContainer();

		if (triggerTime <= previousStateDuration) {
			policy.setActive(false);
			LOGGER.debug(String.format("Deactivate Policy %s as Triggertime is in the past.", policy.getEntityName()));
		} else {
			time.setValue(time.getValue() - previousStateDuration);
			LOGGER.debug(String.format("Reduce Triggertime of Policy %s by %f to %f.", policy.getEntityName(),
					previousStateDuration, time.getValue()));
		}
	}
}
