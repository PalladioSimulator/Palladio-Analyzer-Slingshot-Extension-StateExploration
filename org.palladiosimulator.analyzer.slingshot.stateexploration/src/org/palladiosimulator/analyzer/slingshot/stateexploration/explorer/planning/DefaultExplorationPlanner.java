package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ProactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ReactiveReconfiguration;
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
	// private final ScalingPolicyConcerns changeApplicator;
	private final CutOffConcerns cutOffConcerns;

	public DefaultExplorationPlanner(final DefaultGraph graph) {
		this.rawgraph = graph;
		// this.changeApplicator = new ScalingPolicyConcerns();
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

		this.reduceSimulationTimeTriggerExpectedTime(end.getArchitecureConfiguration().getSPD(), start.getDuration());

		return createConfigBasedOnChange(next.getChange(), start, end);

	}

	/**
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
			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, null, null);
		}

		if (change.get() instanceof final ReactiveReconfiguration reactiveReconf) {
			LOGGER.debug("Reactive Reconfiguration : Update Target Group");

			final ModelAdjustmentRequested initEvent = (new AdjustorEventConcerns(end.getArchitecureConfiguration()))
					.copy(reactiveReconf.getReactiveReconfigurationEvent());
			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, null, initEvent);
		}

		if (change.get() instanceof final ProactiveReconfiguration reconf) {
			LOGGER.debug("Proactive Reconfiguration : create scalingpolicy for one time usage");

			final ModelAdjustmentRequested initEvent = (new AdjustorEventConcerns(end.getArchitecureConfiguration()))
					.copy(reconf.getReactiveReconfigurationEvent());

			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, null, initEvent);

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
		// NOP Always
		this.rawgraph.addFringeEdge(new ToDoChange(Optional.empty(), start));
		// Reactive Reconfiguration - Always.
		if (start.getSnapshot().getModelAdjustmentRequestedEvent().isPresent()) {
			final ModelAdjustmentRequested event = start.getSnapshot().getModelAdjustmentRequestedEvent().get();

			// reactive reconf to the next state.
			this.rawgraph.addFringeEdge(new ToDoChange(Optional.of(new ReactiveReconfiguration(event)), start));

			// reactive reconf at the previous state -> proactive reconf.
			final Optional<ToDoChange> proactiveChange = createProactiveChange(start);

			if (proactiveChange.isPresent()) {
				this.rawgraph.addFringeEdge(proactiveChange.get());
			}
		}

	}

	/**
	 * Create a {@code ToDoChange} that applies the reactive reconfiguration, on
	 * which {@code state} ended onto the first predecessor, which has no successor,
	 * that starts on the application of that policy.
	 * 
	 * Problem: disregards changes, that are still in the fringe. --> also check
	 * fringe!
	 * 
	 * @param state
	 * @return
	 */
	private Optional<ToDoChange> createProactiveChange(final DefaultState state) {

		final ModelAdjustmentRequested event = state.getSnapshot().getModelAdjustmentRequestedEvent().get();

		Optional<DefaultState> predecessor = getPredecessor(state);

		while (predecessor.isPresent() && policyAlreadyExploredAtState(predecessor.get(), event.getScalingPolicy())) {
			predecessor = getPredecessor(predecessor.get());
		}

		if (predecessor.isPresent()) {
			return Optional.of(new ToDoChange(Optional.of(new ProactiveReconfiguration(event)), predecessor.get()));
		} else {
			return Optional.empty();
		}

	}

	/**
	 * Check whether {@code state} already transitioned to a successor, via the
	 * given policy. Or wether it is already planned to explore that (i.e.
	 * corresponding change in the fringe)
	 * 
	 * @param state
	 * @param policy
	 * @return
	 */
	private boolean policyAlreadyExploredAtState(final DefaultState state, final ScalingPolicy policy) {
		List<Change> foo = state.getOutTransitions().stream().filter(t -> t.getChange().isPresent())
				.map(t -> t.getChange().get()).filter(c -> isSamePolicy(c, policy)).toList();

		List<ToDoChange> bar = this.rawgraph.getFringe().stream().filter(c -> c.getStart().equals(state)
				&& c.getChange().isPresent() && isSamePolicy(c.getChange().get(), policy)).toList();

		return !(foo.isEmpty() && bar.isEmpty());
	}

	/**
	 * Check whether the given change is a change for the given policy. Checks via
	 * ID, as the object migh differ.
	 * 
	 * @param c1
	 * @param policy
	 * @return
	 */
	private static boolean isSamePolicy(final Change c1, final ScalingPolicy policy) {
		String idPolicy1 = null;

		if (c1 instanceof ReactiveReconfiguration r) {
			idPolicy1 = r.getReactiveReconfigurationEvent().getScalingPolicy().getId();
		} else if (c1 instanceof ProactiveReconfiguration r) {
			idPolicy1 = r.getReactiveReconfigurationEvent().getScalingPolicy().getId();
		}

		return policy.getId().equals(idPolicy1);
	}

	/**
	 * Get predecessor of the given state.
	 * 
	 * @param state
	 * @return
	 */
	private Optional<DefaultState> getPredecessor(final DefaultState state) {
		Optional<RawModelState> predecessor = rawgraph.getTransitions().stream()
				.filter(t -> t.getTarget().equals(state)).map(t -> t.getSource()).findFirst();

		if (predecessor.isPresent()) {
			return Optional.of((DefaultState) predecessor.get());
		} else {
			return Optional.empty();
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
