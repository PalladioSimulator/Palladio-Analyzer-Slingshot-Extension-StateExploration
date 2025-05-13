package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.CalculatorRegistered;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.ExploredStateBuilder;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.Repository.Repository;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

/**
 *
 * Behavioural Extension for putting more values into the
 * {@link ExploredStateBuilder}.
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = CalculatorRegistered.class, then = {})
@OnEvent(when = SnapshotFinished.class, then = SimulationFinished.class)
public class SnapshotStateUpdateBehaviour implements SimulationBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(SnapshotStateUpdateBehaviour.class);

	/* Configurations */
	private final SnapshotConfiguration snapshotConfig;
	private final SimuComConfig simuComConfig;

	/* State representing current simulation run */
	private final ExploredStateBuilder stateBuilder;

	/**
	 * 
	 * @param stateBuilder   for saving the collected state information
	 * @param snapshotConfig for access to additional information (e.g. min.
	 *                       duration)
	 * @param simuComConfig  for access to the experiment settings
	 */
	@Inject
	public SnapshotStateUpdateBehaviour(final @Nullable ExploredStateBuilder stateBuilder,
			final @Nullable SnapshotConfiguration snapshotConfig, final @Nullable SimuComConfig simuComConfig) {

		this.stateBuilder = stateBuilder;
		this.snapshotConfig = snapshotConfig;
		this.simuComConfig = simuComConfig;
	}

	@Override
	public boolean isActive() {
		return stateBuilder != null && snapshotConfig != null && simuComConfig != null;
	}

	/**
	 * Add access to all measurements to the raw state.
	 *
	 * Subscribes to {@link CalculatorRegistered} because the experiment settings
	 * are created during calculator registration.
	 * 
	 * Keep in mind: in this operation we save a reference to the experiment
	 * settings, but at this point in time the settings are still empty. The actual
	 * measurements are only added during the simulation run
	 *
	 * @param calculatorRegistered signifies that the {@link ExperimentSetting} are
	 *                             now created.
	 */
	@Subscribe
	public void onCalculatorRegistered(final CalculatorRegistered calculatorRegistered) {

		// TODO Skip, if already set!

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

		this.stateBuilder.setExperimentSetting(settings.get(0));
	}

	/**
	 * 
	 * Add the {@link Snapshot} object and the state duration to the builder.
	 * 
	 * Also add the {@link SPDAdjustorStateValues}s to the snapshot. 
	 * 
	 * Once this operation was executed, the state builder is complete.
	 *
	 * @param event signifies that the snapshot is complete.
	 * @return {@link SimulationFinished} event to trigger the end of the current
	 *         simulation run.
	 */
	@Subscribe
	public Result<SimulationFinished> onSnapshotFinished(final SnapshotFinished event) {
		stateBuilder.setSnapshot(event.getEntity());
		stateBuilder.setDuration(event.time());

		this.refineReasonsToLeave(event);

		// Do not build the state. The state will be build in the explorer.

		return Result.of(new SimulationFinished());
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
			stateBuilder.addReasonToLeave(ReasonToLeave.reactiveReconfiguration);
		}
		if (event.time() == snapshotConfig.getMinDuration()) {
			stateBuilder.addReasonToLeave(ReasonToLeave.interval);
		}
	}

}
