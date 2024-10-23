package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateInitialized;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
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
import org.palladiosimulator.spd.constraints.policy.CooldownConstraint;
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
			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, null,
					this.createStateInitEvents(start.getAdjustorStateValues()), start.getId());
		}

		if (change.get() instanceof final Reconfiguration reconf) {
			LOGGER.debug("Create InitConfiguration for Reconfiguration (Pro- or Reactive)");

			final Collection<SPDAdjustorStateValues> initValues = updateInitValues(reconf.getAppliedPolicy(),
					start.getAdjustorStateValues());

			final ModelAdjustmentRequested initEvent = (new AdjustorEventConcerns(end.getArchitecureConfiguration()))
					.copy(reconf.getReactiveReconfigurationEvent());

			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, initEvent,
					this.createStateInitEvents(initValues),
					start.getId());
		}

		throw new UnsupportedOperationException("Environment Change not yet supported.");
	}

	/**
	 *
	 * Adapt initialisation values to represent the application of the reactive or
	 * proactive reconfigurations.
	 *
	 * Reconfigurations at the beginning of the next simulation run are applied by
	 * injecting the respective {@link ModelAdjustmentRequested} event. Thus the
	 * adjustment request does not pass the trigger chain, and the policy
	 * application is not represented in the state of the SPD adjustor.
	 *
	 * To mitigate this, this operation manually updates the counters and times in
	 * the initialisation values. This is necessary for both proactive and reactive
	 * reconfiguration, as the {@code DefaultState} always holds the state of the
	 * adjustors prior to reconfiguration application.
	 *
	 * @param policy     policy to be applied at the beginning of the next
	 *                   simulation run.
	 * @param initValues values to initialise the {@code SPDAdjustorContext} on.
	 * @return collection of update values.
	 */
	private Collection<SPDAdjustorStateValues> updateInitValues(final ScalingPolicy policy,
			final Collection<SPDAdjustorStateValues> initValues) {

		final Collection<SPDAdjustorStateValues> rvals = new HashSet<>(initValues);

		final Optional<CooldownConstraint> cooldownConstraint = policy.getPolicyConstraints().stream()
				.filter(CooldownConstraint.class::isInstance).map(CooldownConstraint.class::cast).findAny();

		if (cooldownConstraint.isEmpty()) {
			return rvals;
		}

		int numberscales = 0;
		double cooldownEnd = 0;
		int numberscalesinCD = 0;

		final Optional<SPDAdjustorStateValues> matchingValues = initValues.stream()
				.filter(v -> v.scalingPolicyId().equals(policy.getId())).findAny();

		if (matchingValues.isPresent()) {
			numberscales = matchingValues.get().numberScales();
			cooldownEnd = matchingValues.get().coolDownEnd();
			numberscalesinCD = matchingValues.get().numberOfScalesInCooldown();
			rvals.remove(matchingValues.get());
		}

		if (numberscalesinCD < cooldownConstraint.get().getMaxScalingOperations()) {
			numberscalesinCD++;
		} else {
			numberscalesinCD = 0;
			cooldownEnd = cooldownConstraint.get().getCooldownTime();
		}

		final SPDAdjustorStateValues newvalues = new SPDAdjustorStateValues(policy.getId(), 0.0, numberscales + 1,
				cooldownEnd, numberscalesinCD);

		rvals.add(newvalues);

		return rvals;
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
		final ToDoChange change = new ToDoChange(Optional.empty(), start);
		if (matchesPattern(change)) {
			return; // drop the entire branch.
		}
		this.fringe.add(change);
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
		return previous.getDuration() < minDuration ? minDuration : 2 * previous.getDuration();
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

	private Collection<DESEvent> createStateInitEvents(
			final Collection<SPDAdjustorStateValues> values) {
		return values.stream().map(value -> (DESEvent) new SPDAdjustorStateInitialized(value)).toList();
	}

	/**
	 * Patter is "leav on rea" (prev) -> NOOP -> "leav on rea" (current) -> NOOP
	 * (ToDoChange)
	 *
	 * @param current
	 * @return
	 */
	private static boolean matchesPattern(final ToDoChange change) {

		final DefaultState current = change.getStart();

		if (current.getEndTime() == 0.0) { // root???
			return false;
		}

		final DefaultState prev = (DefaultState) current.getIncomingTransition().getSource();

		return samePolicy(current, prev) && bothNOOP(change, current.getIncomingTransition());
	}

	private static boolean bothNOOP(final ToDoChange current, final RawTransition prev) {
		return current.getChange().isEmpty() && prev.getChange().isEmpty();
	}

	private static boolean samePolicy(final DefaultState current, final DefaultState prev) {
		if (current.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()
				|| prev.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()) {
			return false;
		}
		final ScalingPolicy policyCurrent = current.getSnapshot().getModelAdjustmentRequestedEvent().get()
				.getScalingPolicy();
		final ScalingPolicy policyPrev = prev.getSnapshot().getModelAdjustmentRequestedEvent().get().getScalingPolicy();

		return policyCurrent.getId().equals(policyPrev.getId());
	}
}
