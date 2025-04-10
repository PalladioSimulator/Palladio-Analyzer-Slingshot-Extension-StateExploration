package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateInitialized;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.cost.events.TakeCostMeasurement;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.CalculatorRegistered;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultStateBuilder;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.Repository.Repository;
import org.palladiosimulator.spd.ScalingPolicy;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

/**
 *
 * Behavioural Extension to handle everything related to the RawGraphState.
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = CalculatorRegistered.class, then = {})
@OnEvent(when = SnapshotFinished.class, then = SimulationFinished.class)
@OnEvent(when = SPDAdjustorStateInitialized.class, then = {})
@OnEvent(when = SnapshotInitiated.class, then = { TakeCostMeasurement.class }, cardinality = EventCardinality.MANY)
public class SnapshotStateUpdateBehaviour implements SimulationBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(SnapshotStateUpdateBehaviour.class);

	/* Configurations */
	private final SnapshotConfiguration snapshotConfig;
	private final SimuComConfig simuComConfig;

	/* State representing current simulation run */
	private final DefaultStateBuilder halfDoneState;
	/* Snapshotted events taken from earlier simulation run */

	private final Map<String, SPDAdjustorStateValues> policyIdToValues;


	private final boolean activated;

	@Inject
	public SnapshotStateUpdateBehaviour(final @Nullable DefaultStateBuilder halfDoneState,
			final @Nullable SnapshotConfiguration snapshotConfig, final @Nullable EventsToInitOnWrapper eventsWrapper,
			final @Nullable SimuComConfig simuComConfig) {

		this.activated = halfDoneState != null && snapshotConfig != null && simuComConfig != null;

		this.halfDoneState = halfDoneState;
		this.snapshotConfig = snapshotConfig;
		this.simuComConfig = simuComConfig;

		this.policyIdToValues = new HashMap<>();
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}


	/**
	 * Add the measurements to the raw state.
	 *
	 * Subscribes to {@link CalculatorRegistered} because the experiment settings
	 * are created during calculator registration.
	 *
	 * @param calculatorRegistered
	 */
	@Subscribe
	public void onCalculatorRegistered(final CalculatorRegistered calculatorRegistered) {

		// SKip, if already set!

		final List<Repository> repos = RepositoryManager.getCentralRepository().getAvailableRepositories();

		final Optional<Repository> repo = repos.stream().filter(r -> !r.getExperimentGroups().isEmpty()).findFirst();

		if (repo.isEmpty()) {
			throw new IllegalStateException("Repository is missing.");
		}

		final List<ExperimentGroup> groups = repo.get().getExperimentGroups().stream()
				.filter(g -> g.getPurpose().equals(this.simuComConfig.getNameExperimentRun()))
				.collect(Collectors.toList());

		if (groups.size() != 1) {
			throw new IllegalStateException(
					String.format("Wrong number of matching Experiment Groups. should be 1 but is %d", groups.size()));
		}

		final List<ExperimentSetting> settings = groups.get(0).getExperimentSettings().stream()
				.filter(s -> s.getDescription().equals(this.simuComConfig.getVariationId()))
				.collect(Collectors.toList());

		if (settings.size() != 1) {
			throw new IllegalStateException(String.format(
					"Wrong number of Experiment Settings matching the variation id. should be 1 but is %d",
					settings.size()));
		}

		this.halfDoneState.setExperimentSetting(settings.get(0));
	}

	/**
	 *
	 * NB : state builder is complete.
	 *
	 * @param event
	 * @return
	 */
	@Subscribe
	public Result<SimulationFinished> onSnapshotFinished(final SnapshotFinished event) {
		halfDoneState.setSnapshot(event.getEntity());
		halfDoneState.setDuration(event.time());

		this.refineReasonsToLeave(event);

		this.policyIdToValues.values().stream().map(s -> this.setOffsets(s, event.time()))
				.forEach(s -> event.getEntity().addSPDAdjustorStateValues(s));

		// also offset targetgroup state values. --> ???

		// Do not build the state. The state will be build in the explorer.

		return Result.of(new SimulationFinished());
	}

	/**
	 * Subscribe to the {@link SPDAdjustorStateInitialized} events, because we also
	 * need those states for the next simulation.
	 *
	 * @param event
	 */
	@Subscribe
	public void onAdjustorStateUpdated(final SPDAdjustorStateInitialized event) {
		this.policyIdToValues.put(event.getStateValues().scalingPolicyId(), event.getStateValues());
	}

	/**
	 *
	 * Refines the reasons to leave by adding missing reasons based on the snapshot.
	 *
	 * If a snapshot gets triggered due to {@link ReasonToLeave#closenessToSLO},
	 * while at the same point in time a adjustment was triggered, the adjustment is
	 * also a reason to leave, but not yet represented in the reasons to leave set.
	 *
	 * Also, if no external trigger happened, {@link ReasonToLeave#interval} must be
	 * added.
	 *
	 * @param snapshot snapshot of current state
	 */
	private void refineReasonsToLeave(final SnapshotFinished event) {
		if (!event.getEntity().getModelAdjustmentRequestedEvent().isEmpty()) {
			halfDoneState.addReasonToLeave(ReasonToLeave.reactiveReconfiguration);
		}
		if (event.time() == snapshotConfig.getMinDuration()) {
			halfDoneState.addReasonToLeave(ReasonToLeave.interval);
		}
	}

	/**
	 * Adjust the time of the latest adjustment and the time of the cooldown to the
	 * reference time.
	 *
	 * If the latest adjustment was at t = 5 s, the cooldown ends at t = 15 s, and
	 * the reference time is t = 10 s, then the adjusted values will be latest
	 * adjustment at t = -5 s and cooldown end at t = 5 s.
	 *
	 * @param stateValues   values to be adjusted
	 * @param referenceTime time to adjust to.
	 * @return adjusted values.
	 */
	private SPDAdjustorStateValues setOffsets(final SPDAdjustorStateValues stateValues, final double referenceTime) {
		final double latestAdjustmentAtSimulationTime = stateValues.latestAdjustmentAtSimulationTime() - referenceTime;
		final int numberScales = stateValues.numberScales();
		final double coolDownEnd = stateValues.coolDownEnd() > 0.0 ? stateValues.coolDownEnd() - referenceTime : 0.0;
		final int numberOfScalesInCooldown = stateValues.numberOfScalesInCooldown();

		final List<ScalingPolicy> enactedPolicies = new ArrayList<>(stateValues.enactedPolicies()); // unchanged
		final List<Double> enactmentTimeOfPolicies = stateValues.enactmentTimeOfPolicies().stream()
				.map(time -> time - referenceTime).toList();

		return new SPDAdjustorStateValues(stateValues.scalingPolicy(), latestAdjustmentAtSimulationTime, numberScales,
				coolDownEnd, numberOfScalesInCooldown, enactedPolicies, enactmentTimeOfPolicies);
	}
}
