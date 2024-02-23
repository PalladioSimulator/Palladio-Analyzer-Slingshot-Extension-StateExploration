package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.Optional;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.SetBasedArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ReactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration.SimulationInitConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultTransition;
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
 * @author stiesssh
 *
 */
public class DefaultExplorationPlanner {

	private static final Logger LOGGER = Logger.getLogger(DefaultExplorationPlanner.class.getName());

	private final DefaultGraph rawgraph;
	private final ScalingPolicyConcerns changeApplicator;
	private final AdjustorEventConcerns adjustorEventConcerns;
	private final CutOffConcerns cutOffConcerns;

	public DefaultExplorationPlanner(final SPD spd, final DefaultGraph graph) {
		this.rawgraph = graph;
		this.changeApplicator = new ScalingPolicyConcerns();
		this.adjustorEventConcerns = new AdjustorEventConcerns();
		this.cutOffConcerns = new CutOffConcerns();

		this.updateGraphFringePostSimulation(graph.getRoot());
	}

	/**
	 *
	 *
	 * @return Configuration for the next simulation run
	 */
	public SimulationInitConfiguration createConfigForNextSimualtionRun() {
		assert this.rawgraph.hasNext();

		ToDoChange next = this.rawgraph.getNext();

		while (!this.cutOffConcerns.shouldExplore(next)) {
			LOGGER.debug(String.format("Future %s is bad, wont explore.", next.toString()));
			// TODO : Exception if the entire fringe is bad.
			next = this.rawgraph.getNext();
		}

		final DefaultState start = next.getStart();
		final DefaultState end = this.createNewGraphNode(next);

		final double duration = calculateRunDuration(start);

		this.reduceSimulationTimeTriggerExpectedTime(end.getArchitecureConfiguration().getSPD(), start.getDuration());

		// different handling depending of type of change.
		if (next.getChange().isEmpty()) {
			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, null, null);
		}

		if (next.getChange().get() instanceof final ReactiveReconfiguration reactiveReconf) {
			LOGGER.debug("Reactive Reconfiguration : Update Target Group");

			final DESEvent initEvent = this.adjustorEventConcerns.copyForTargetGroup(
					reactiveReconf.getReactiveReconfigurationEvent(), end.getArchitecureConfiguration());
			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, null, initEvent);
		}

		if (next.getChange().get() instanceof final Reconfiguration reconf) {
			LOGGER.debug("Proactive Reconfiguration : create scalingpolicy for one time usage");

			final ScalingPolicy initPolicy = this.changeApplicator
					.createOneTimeUsageScalingPolicy(reconf.getScalingPolicy(), end.getArchitecureConfiguration());
			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, initPolicy, null);
		}

		throw new UnsupportedOperationException("Environment Change not yet supported.");
	}

	/**
	 * Add new exploration directions to the graph fringe.
	 *
	 * To be called after the given state was simulated.
	 *
	 * @param start
	 */
	public void updateGraphFringePostSimulation(final DefaultState start) {
		// NOP
		this.rawgraph.addFringeEdge(new ToDoChange(Optional.empty(), start));
		// Reactive Reconfiguration
		if (start.getSnapshot().getModelAdjustmentRequestedEvent().isPresent()) {
			final DESEvent event = start.getSnapshot().getModelAdjustmentRequestedEvent().get();

			this.rawgraph.addFringeEdge(
					new ToDoChange(Optional.of(new ReactiveReconfiguration(event)), start));
		}
		// Proactive Reconfiguration
		for (final ScalingPolicy scalingPolicy : this.changeApplicator.getExplorationPolicyTemplates()) {
			this.rawgraph.addFringeEdge(new ToDoChange(Optional.of(new Reconfiguration(scalingPolicy)), start));
		}
	}

	/**
	 * Create a new graph note with a new arch configuration.
	 *
	 * creating a fully connected Graph node encompasses :
	 * - copying architecture configuration from preceding state.
	 * - setting the start time of the new node wrt. global time.
	 * - adding the node to the graph's node list.
	 * - creating transition to connect new node t predecessor.
	 *
	 * @return a new node, connected to its predecessor in graph.
	 */
	private DefaultState createNewGraphNode(final ToDoChange next) {
		final DefaultState predecessor = next.getStart();

		final SetBasedArchitectureConfiguration newConfig = predecessor.getArchitecureConfiguration().copy();
		final DefaultState newNode = new DefaultState(predecessor.getEndTime(), newConfig);

		this.rawgraph.addNode(newNode);

		final DefaultTransition nextTransition = new DefaultTransition(next.getChange(), predecessor, newNode);
		predecessor.addOutTransition(nextTransition);

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
			return 21.5; // Evil. Should be something scraped from launch configuration
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

		// get all triggers on Fixed point in time.
		spd.getScalingPolicies().stream().filter(policy -> policy.isActive()).map(policy -> policy.getScalingTrigger())
				.filter(BaseTrigger.class::isInstance).map(BaseTrigger.class::cast)
				.filter(trigger -> trigger.getStimulus() instanceof SimulationTime)
				.map(trigger -> trigger.getExpectedValue()).filter(ExpectedTime.class::isInstance)
				.map(ExpectedTime.class::cast).forEach(time -> this.updateValue(time, offset));

		ResourceUtils.saveResource(spd.eResource());
	}

	/**
	 * update value helper.
	 *
	 * @param time
	 * @param previousStateDuration
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
