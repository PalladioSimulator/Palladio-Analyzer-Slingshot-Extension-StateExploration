package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.events.PreSimulationConfigurationStarted;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.CalculatorRegistered;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemorySnapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.Repository.Repository;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

/**
 *
 * The ugly snapshot related stuff. i need to reconsider much of this, i guess.
 *
 * @author stiesssh
 *
 */
@OnEvent(when = SimulationFinished.class, then = {})
@OnEvent(when = CalculatorRegistered.class, then = {})
@OnEvent(when = SimulationStarted.class, then = AbstractEntityChangedEvent.class, cardinality = EventCardinality.MANY)
// less specific, but also less dependecies.
@OnEvent(when = SnapshotFinished.class, then = SimulationFinished.class)
@OnEvent(when = PreSimulationConfigurationStarted.class, then = SnapshotInitiated.class)
public class SnapshotStateRelatedBehaviour implements SimulationBehaviorExtension {

	private final SnapshotConfiguration snapshotConfig;
	private final SimuComConfig simuComConfig;

	private final DefaultState halfDoneState;

	private final Snapshot snapToInitOn;

	@Inject
	public SnapshotStateRelatedBehaviour(final DefaultState halfDoneState, final InMemorySnapshot snapToInitOn,
			final SnapshotConfiguration snapshotConfig, final SimuComConfig simuComConfig) {
		this.halfDoneState = halfDoneState;
		this.snapToInitOn = snapToInitOn;
		this.snapshotConfig = snapshotConfig;
		this.simuComConfig = simuComConfig;
	}

	/**
	 *
	 * TODO
	 *
	 * @param configurationStarted
	 * @return
	 */
	@Subscribe
	public Result<?> onConfigurationStarted(final PreSimulationConfigurationStarted configurationStarted) {
		return Result.of(new SnapshotInitiated(this.snapshotConfig.getSnapinterval()));
	}

	/**
	 *
	 * TODO
	 *
	 * @param simulationStarted
	 * @return
	 */
	@Subscribe
	public Result<?> onSimulationStarted(final SimulationStarted simulationStarted) {
		if (!snapshotConfig.isStartFromSnapshot()) {
			return Result.empty();
		}
		final Set<DESEvent> initialEvents = this.snapToInitOn.getEvents();
		return Result.of(initialEvents);
	}

	/**
	 * Add the measurements to the raw state.
	 *
	 * Subscribes to {@link CalculatorRegistered} because the experiment settings
	 * are created during calculator registration.
	 *
	 * @param calculatorRegistered
	 * @return
	 */
	@Subscribe
	public void onCalculatorRegistered(final CalculatorRegistered calculatorRegistered) {

		final List<Repository> repos = RepositoryManager.getCentralRepository().getAvailableRepositories();

		final Optional<Repository> repo = repos.stream().filter(r -> !r.getExperimentGroups().isEmpty()).findFirst();

		if (repo.isEmpty()) {
			throw new IllegalStateException("Repository is missing.");
		}

		final List<ExperimentGroup> groups = repo.get().getExperimentGroups().stream().filter(g -> g.getPurpose().equals(this.simuComConfig.getNameExperimentRun())).collect(Collectors.toList());

		if (groups.size() != 1) {
			throw new IllegalStateException(String.format(
					"Wrong number of matching Experiment Groups. should be 1 but is %d",
					groups.size()));
		}

		final List<ExperimentSetting> settings = groups.get(0).getExperimentSettings()
				.stream().filter(s -> s.getDescription().equals(this.simuComConfig.getVariationId()))
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
	 * NB : state is complete.
	 *
	 * @param event
	 * @return
	 */
	@Subscribe
	public Result<?> onSnapshotFinished(final SnapshotFinished event) {
		halfDoneState.setSnapshot(event.getEntity());
		halfDoneState.setDuration(event.time());
		// Do not add the state anywhere, just finalise it. Assumption is, it already is
		// in the graph.
		return Result.of(new SimulationFinished());
	}
}
