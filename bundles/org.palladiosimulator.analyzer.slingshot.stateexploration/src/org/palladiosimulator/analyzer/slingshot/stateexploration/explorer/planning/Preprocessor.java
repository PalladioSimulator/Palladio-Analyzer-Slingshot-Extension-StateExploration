package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.ArrayList;
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
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ReactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration.SimulationInitConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraphFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.PlannedTransition;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.constraints.policy.CooldownConstraint;
import org.palladiosimulator.spd.triggers.BaseTrigger;
import org.palladiosimulator.spd.triggers.ScalingTrigger;
import org.palladiosimulator.spd.triggers.expectations.ExpectedTime;
import org.palladiosimulator.spd.triggers.stimuli.SimulationTime;

/**
 *
 * The preprocessor creates the {@link SimulationInitConfiguration} for the next
 * simulation run.
 * 
 * The order of simulation of the planned transitions is as defined by the
 * fringe. The preprocessor does not change the order. However, it can drop a
 * planned transition, e.g. because it is now in the past compared to the time
 * in the managed system.
 *
 * @author Sophie Stieß
 *
 */
public class Preprocessor {

	private static final Logger LOGGER = Logger.getLogger(Preprocessor.class.getName());

	private final DefaultGraph rawgraph;
	private final DefaultGraphFringe fringe;
	private final CutOffConcerns cutOffConcerns;

	private final double minDuration;

	public Preprocessor(final DefaultGraph graph, final DefaultGraphFringe fringe, final double minDuration) {
		this.rawgraph = graph;
		this.fringe = fringe;
		this.minDuration = minDuration;
		this.cutOffConcerns = new CutOffConcerns();
	}

	/**
	 *
	 * Ensures, that the state graph node, that will be simulated with the resulting configuration is already connected to the state graph. 
	 * Ensures, that the at max one planned change is removed from the fringe.
	 * 
	 * Ensures, that the SPD model of the new state graph node is updated (e.g. reduced triggertimes for simulation time base triggers, and that the changes are saved to file as well. 
	 * 
	 * Ensures, that the {@link ModelAdjustmentRequested events} get copied and point to the corrcet (?) SPD file.
	 *
	 * @return Configuration for the next simulation run, or empty optional, if
	 *         fringe has no viable change.
	 */
	public Optional<SimulationInitConfiguration> createConfigForNextSimualtionRun() {
		PlannedTransition next = this.fringe.poll();

		while (!this.cutOffConcerns.shouldExplore(next)) {
			LOGGER.debug(String.format("Future %s is bad, won't explore.", next.toString()));
			if (this.fringe.isEmpty()) {
				return Optional.empty();
			}
			next = this.fringe.poll();
		}

		final DefaultState start = next.getStart();
		final DefaultState end = this.createNewGraphNode(next);

		if (end.getArchitecureConfiguration().getSPD().isPresent()) {

			final SPD spd = end.getArchitecureConfiguration().getSPD().get();

			this.updateSimulationTimeTriggeredPolicy(spd, start.getDuration());

			if (next.getChange().isPresent() && next.getChange().get() instanceof ReactiveReconfiguration rea) {
				this.deactivateReactivePolicies(spd, rea);
			}
			ResourceUtils.saveResource(spd.eResource());
		}

		return Optional.of(createConfigBasedOnChange(next.getChange(), start, end));
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
			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, List.of(),
					this.createStateInitEvents(start.getAdjustorStateValues()), start.getId());
		}

		if (change.get() instanceof final Reconfiguration reconf) {
			LOGGER.debug("Create InitConfiguration for Reconfiguration (Pro- or Reactive)");

			final Collection<SPDAdjustorStateValues> initValues = new HashSet<>();

			for (final ScalingPolicy policy : reconf.getAppliedPolicies()) {
				initValues.addAll(updateInitValues(policy, start.getAdjustorStateValues()));
			}

			final List<ModelAdjustmentRequested> initEvents = new ArrayList<>();
			for (final ModelAdjustmentRequested event : reconf.getReactiveReconfigurationEvents()) {
				initEvents.add(new AdjustorEventConcerns(end.getArchitecureConfiguration()).copy(event));
			}

			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, initEvents,
					this.createStateInitEvents(initValues), start.getId());
		}

		throw new UnsupportedOperationException("Environment Change not yet supported.");
	}

	/**
	 * Deactivate all reactively applied policies at the given SPD model, that have
	 * a simulation time based trigger.
	 * 
	 * Usually, it does not help to deactivate the policies directly via the
	 * reconfiguration, because the reconfiguration points to the wrong copy of the
	 * policies.
	 * 
	 * @param spd model to deactivate policies at.
	 * @param rea policies to deactivate
	 */
	private void deactivateReactivePolicies(final SPD spd, ReactiveReconfiguration rea) {
		List<String> ids = rea.getAppliedPolicies().stream().map(p -> p.getId()).toList();
		spd.getScalingPolicies().stream().filter(p -> isSimulationTimeTrigger(p.getScalingTrigger()))
				.filter(p -> ids.contains(p.getId())).forEach(p -> p.setActive(false));
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
	 * Create a new graph note with a new arch configuration.
	 *
	 * creating a fully connected Graph node encompasses : - copying architecture
	 * configuration from preceding state. - setting the start time of the new node
	 * wrt. global time. - adding the node to the graph's node list. - creating
	 * transition to connect new node t predecessor.
	 *
	 * @return a new node, connected to its predecessor in graph.
	 */
	private DefaultState createNewGraphNode(final PlannedTransition next) {
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
	private void updateSimulationTimeTriggeredPolicy(final SPD spd, final double offset) {
		spd.getScalingPolicies().stream()
				.filter(policy -> policy.isActive() && this.isSimulationTimeTrigger(policy.getScalingTrigger()))
				.map(policy -> ((BaseTrigger) policy.getScalingTrigger()))
				.forEach(trigger -> this.updateValue(((ExpectedTime) trigger.getExpectedValue()), offset));
	}

	/**
	 * Update expected time value and deactivate, if the policy is in the past
	 * necessary.
	 * 
	 * @param time                  model element to be updated
	 * @param previousStateDuration duration to subtract from {@code time}.
	 */
	private void updateValue(final ExpectedTime time, final double previousStateDuration) {
		final double triggerTime = time.getValue();

		final ScalingPolicy policy = (ScalingPolicy) time.eContainer().eContainer();

		if (triggerTime < previousStateDuration) {
			policy.setActive(false);
			LOGGER.debug(String.format("Deactivate Policy %s as Triggertime is in the past.", policy.getEntityName()));
		} else {
			time.setValue(time.getValue() - previousStateDuration);
			LOGGER.debug(String.format("Reduce Triggertime of Policy %s by %f to %f.", policy.getEntityName(),
					previousStateDuration, time.getValue()));
		}
	}

	/**
	 * Check whether the given trigger is based on {@link SimulationTime} and
	 * {@link ExpectedTime}.
	 * 
	 * TODO take compound triggers into consideration.
	 * 
	 * @param trigger trigger to be checked.
	 * @return true iff the trigger is based on {@link SimulationTime} and
	 *         {@link ExpectedTime}
	 */
	private boolean isSimulationTimeTrigger(ScalingTrigger trigger) {
		return trigger instanceof BaseTrigger base && base.getStimulus() instanceof SimulationTime
				&& base.getExpectedValue() instanceof ExpectedTime;
	}

	/**
	 * Create events for initialising the state inside the SPD Interpreter.
	 * 
	 * @param values values from previous simulation run.
	 * @return Events to initialise the interpreter to the states from the previous
	 *         run.
	 */
	private Collection<DESEvent> createStateInitEvents(final Collection<SPDAdjustorStateValues> values) {
		return values.stream().map(value -> (DESEvent) new SPDAdjustorStateInitialized(value)).toList();
	}

}
