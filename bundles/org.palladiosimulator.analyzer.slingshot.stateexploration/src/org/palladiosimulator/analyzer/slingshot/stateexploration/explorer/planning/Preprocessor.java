package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateInitialized;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ReactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration.SimulationInitConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.fringe.FringeFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.ExploredState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.ExploredStateBuilder;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.PlannedTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.StateGraph;
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

	private final StateGraph rawgraph;
	private final FringeFringe fringe;
	private final CutOffConcerns cutOffConcerns;

	private final double minDuration;

	public Preprocessor(final StateGraph graph, final FringeFringe fringe, final double minDuration) {
		this.rawgraph = graph;
		this.fringe = fringe;
		this.minDuration = minDuration;
		this.cutOffConcerns = new CutOffConcerns(graph, minDuration);
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

		final ExploredState start = next.getStart();
		final ExploredStateBuilder end = new ExploredStateBuilder(this.rawgraph, next);

		if (end.getStartupInformation().architecureConfiguration().getSPD().isPresent()) {

			final SPD spd = end.getStartupInformation().architecureConfiguration().getSPD().get();

			this.updateSimulationTimeTriggeredPolicy(spd, start.getDuration());

			if (next.getChange().isPresent() && next.getChange().get() instanceof final ReactiveReconfiguration rea) {
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
			final ExploredState start, final ExploredStateBuilder end) {

		final double duration = this.minDuration;

		if (change.isEmpty()) {
			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, List.of(),
					this.createStateInitEvents(start.getSnapshot().getSPDAdjustorStateValues()));
		}

		if (change.get() instanceof final Reconfiguration reconf) {
			LOGGER.debug("Create InitConfiguration for Reconfiguration (Pro- or Reactive)");

			final Collection<SPDAdjustorStateValues> initValues = new HashSet<>();

			for (final ScalingPolicy policy : reconf.getAppliedPolicies()) {
				initValues.addAll(updateInitValues(policy, start.getSnapshot().getSPDAdjustorStateValues()));
			}

			final List<ModelAdjustmentRequested> initEvents = new ArrayList<>();
			for (final ModelAdjustmentRequested event : reconf.getReactiveReconfigurationEvents()) {
				initEvents.add(new AdjustorEventConcerns(end.getStartupInformation().architecureConfiguration()).copy(event));
			}

			return new SimulationInitConfiguration(start.getSnapshot(), end, duration, initEvents,
					this.createStateInitEvents(initValues));
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
	private void deactivateReactivePolicies(final SPD spd, final ReactiveReconfiguration rea) {
		final List<String> ids = rea.getAppliedPolicies().stream().map(p -> p.getId()).toList();
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

		Collection<SPDAdjustorStateValues> rvals = new HashSet<>(initValues);

		final Optional<CooldownConstraint> cooldownConstraint = policy.getPolicyConstraints().stream()
				.filter(CooldownConstraint.class::isInstance).map(CooldownConstraint.class::cast).findAny();

		int numberscales = 0;
		double cooldownEnd = 0;
		int numberscalesinCD = 0;
		
		List<ScalingPolicy> enactedPolicies = new ArrayList<>();
		List<Double> enactmentTimeOfPolicies = new ArrayList<>();

		final Optional<SPDAdjustorStateValues> policyMatch = initValues.stream()
				.filter(v -> v.scalingPolicyId().equals(policy.getId())).findAny();
		

		final Optional<SPDAdjustorStateValues> targetgroupMatch = initValues.stream()
				.filter(v -> v.scalingPolicy().getTargetGroup().getId().equals(policy.getTargetGroup().getId())).findAny();

		if (policyMatch.isPresent()) {
			numberscales = policyMatch.get().numberScales();
			cooldownEnd = policyMatch.get().coolDownEnd();
			numberscalesinCD = policyMatch.get().numberOfScalesInCooldown();

			rvals.remove(policyMatch.get());
		}

		if (targetgroupMatch.isPresent()) {
			enactedPolicies = new ArrayList<>(targetgroupMatch.get().enactedPolicies());
			enactmentTimeOfPolicies = new ArrayList<>(targetgroupMatch.get().enactmentTimeOfPolicies());
			
			// for all init records that target the same target group, remove the target group updates.
			rvals = new ArrayList<>(rvals.stream().map(val -> new SPDAdjustorStateValues(val.scalingPolicy(), val.latestAdjustmentAtSimulationTime(), val.numberScales(),
					val.coolDownEnd(), val.numberOfScalesInCooldown(), List.of(), List.of())).toList());
		}

		if (cooldownConstraint.isPresent()) {
			if (numberscalesinCD < cooldownConstraint.get().getMaxScalingOperations()) {
				numberscalesinCD++;
			} else {
				numberscalesinCD = 0;
				cooldownEnd = cooldownConstraint.get().getCooldownTime();
			}
		}
		
		enactmentTimeOfPolicies.add(0.0);
		enactedPolicies.add(policy);

		// only the new/updated init record (i.e. the one for the most recent policy) holds the init values for the targetgroup. 
		final SPDAdjustorStateValues newvalues = new SPDAdjustorStateValues(policy, 0.0, numberscales + 1,
				cooldownEnd, numberscalesinCD, enactedPolicies, enactmentTimeOfPolicies); 

		rvals.add(newvalues);

		return rvals;
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
	private boolean isSimulationTimeTrigger(final ScalingTrigger trigger) {
		return trigger instanceof final BaseTrigger base && base.getStimulus() instanceof SimulationTime
				&& base.getExpectedValue() instanceof ExpectedTime;
	}

	/**
	 * Create events for initialising the state inside the SPD Interpreter.
	 * 
	 * @param values values from previous simulation run.
	 * @return Events to initialise the interpreter to the states from the previous
	 *         run.
	 */
	private Set<SPDAdjustorStateInitialized> createStateInitEvents(final Collection<SPDAdjustorStateValues> values) {
		return values.stream().map(value -> new SPDAdjustorStateInitialized(value)).collect(Collectors.toSet());
	}

}
