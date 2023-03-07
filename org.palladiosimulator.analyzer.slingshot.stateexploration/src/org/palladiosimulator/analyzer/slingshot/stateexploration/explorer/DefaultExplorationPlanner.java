package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.ArrayDeque;
import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ToDoChange;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * The ExplorationPlanner decides on the direction of the exploration.
 *
 * @author stiesssh
 *
 */
public class DefaultExplorationPlanner {

	private final DefaultGraph rawgraph;
	private final ArrayDeque<ScalingPolicy> policies;
	private final ChangeApplicator changeApplicator;

	public DefaultExplorationPlanner(final SPD spd, final DefaultGraph graph) {
		this.policies = new ArrayDeque<>(spd.getScalingPolicies());
		this.rawgraph = graph;
		this.changeApplicator = new ChangeApplicator();

		final DefaultState root = graph.getRoot();

		this.rawgraph.addFringeEdge(new ToDoChange(Optional.empty(), root));
		for (final ScalingPolicy scalingPolicy : policies) {
			this.rawgraph.addFringeEdge(new ToDoChange(Optional.of(new Reconfiguration(scalingPolicy)), root));
		}
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
		final SimulationInitConfiguration config = new SimulationInitConfiguration(start.getSnapshot(), end, duration);

		return config;
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
	 * Create a new graph note with a new arch configuration.
	 *
	 * Also add the new node to the graph and set the transitions that lead into the
	 * new node.
	 *
	 * @return a new node, already connected to the graph.
	 */
	private DefaultState createNewGraphNode(final ToDoChange next) {
		final DefaultState start = next.getStart();

		final ArchitectureConfiguration newConfig = this.changeApplicator.createNewArchConfig(next.getStart(), next);

		final DefaultState end = new DefaultState(start.getEndTime(), newConfig);
		this.rawgraph.addNode(end);

		final DefaultTransition nextTransition = new DefaultTransition(next.getChange(), start, end);
		// TODO set the model diff between start and end state in the transition.

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
			return 30.0; // Evil. Should be something scraped from launch configuration
		}
		return previous.getDuration();
	}
}
