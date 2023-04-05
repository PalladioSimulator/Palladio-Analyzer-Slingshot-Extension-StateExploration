package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ReactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
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
	private final ArrayDeque<ScalingPolicy> policies;
	private final ChangeApplicator changeApplicator;

	public DefaultExplorationPlanner(final SPD spd, final DefaultGraph graph) {
		this.policies = new ArrayDeque<>(spd.getScalingPolicies().stream().filter(policy -> policy.isActive()).collect(Collectors.toList()));
		this.rawgraph = graph;
		this.changeApplicator = new ChangeApplicator();

		final DefaultState root = graph.getRoot();

		this.updateGraphFringe(root);
	}

	/**
	 *
	 *
	 * @return Configuration for the next simulation run
	 */
	public SimulationInitConfiguration createConfigForNextSimualtionRun() {
		final ToDoChange next = this.rawgraph.getNext();

		final DefaultState start = next.getStart();
		final DefaultState end = this.createNewGraphNode(next);

		this.updateGraphFringe(end);

		final double duration = calculateRunDuration(start);

		this.updateSPD(end.getArchitecureConfiguration().getSPD(), start.getDuration());

		// different handling depending of type of change.
		if (next.getChange().isEmpty()) {
			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, null, null);
		}

		if (next.getChange().get() instanceof final ReactiveReconfiguration reactiveReconf) {
			LOGGER.debug("Reactive Reconfiguration : Update Target Group");

			final DESEvent initEvent = this.changeApplicator.updateTargetGroup(
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
	 * Insert possible changes, out going from the given node into the fringe.
	 *
	 * Currently, insert an NOP and one todo for each known scaling policy
	 *
	 * Actually, i've always wondered how to get reactive reconfiguration out of the
	 * simulation, and this kinda connects to this fringe thing...and i have
	 * thoughts but cant find the words right now.
	 *
	 * @param start
	 */
	private void updateGraphFringe(final DefaultState start) {
		this.rawgraph.addFringeEdge(new ToDoChange(Optional.empty(), start));
		for (final ScalingPolicy scalingPolicy : policies) {
			this.rawgraph.addFringeEdge(new ToDoChange(Optional.of(new Reconfiguration(scalingPolicy)), start));
		}
	}

	/**
	 * Temporary helper
	 *
	 * TODO : restructure
	 *
	 * @param start
	 */
	public void updateGraphFringePostSimulation(final DefaultState start) {
		if (start.getSnapshot().getAdjustorEvent().isPresent()) {
			final DESEvent event = start.getSnapshot().getAdjustorEvent().get();
			this.rawgraph.addFringeEdge(new ToDoChange(Optional.of(new ReactiveReconfiguration(event)), start));
		}
	}

	/**
	 * Create a new graph note with a new arch configuration.
	 *
	 * Also add the new node to the graph and set the transitions that lead into the
	 * new node.
	 *
	 * @return a new node, already connected to the graph.
	 */
	private DefaultState createNewGraphNode(final ToDoChange next) {
		final DefaultState start = next.getStart();

		final ArchitectureConfiguration newConfig = start.getArchitecureConfiguration().copy();

		final DefaultState end = new DefaultState(start.getEndTime(), newConfig);

		this.rawgraph.addNode(end);

		final DefaultTransition nextTransition = new DefaultTransition(next.getChange(), start, end);

		start.addOutTransition(nextTransition);

		return end;
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
	 * reduces the expected value for scaling policies with simulation time stimulus
	 * triggers, or deactivates the policy if the trigger is in the past with regard
	 * to global time.
	 *
	 * @param spd
	 * @param offset
	 */
	private void updateSPD(final SPD spd, final double offset) {

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
