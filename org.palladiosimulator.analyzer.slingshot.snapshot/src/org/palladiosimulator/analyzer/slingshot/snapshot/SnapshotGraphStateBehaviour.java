package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.events.PreSimulationConfigurationStarted;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.CalculatorRegistered;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
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
 * Behavioural Extension to handle everything related to the  RawGraphState.
 *
 * @author stiesssh
 *
 */
@OnEvent(when = CalculatorRegistered.class, then = {})
@OnEvent(when = SimulationStarted.class, then = AbstractEntityChangedEvent.class, cardinality = EventCardinality.MANY)
@OnEvent(when = SnapshotFinished.class, then = SimulationFinished.class)
@OnEvent(when = PreSimulationConfigurationStarted.class, then = SnapshotInitiated.class)
public class SnapshotGraphStateBehaviour implements SimulationBehaviorExtension {

	private final Logger LOGGER = Logger.getLogger(SnapshotGraphStateBehaviour.class);

	/* Configurations */
	private final SnapshotConfiguration snapshotConfig;
	private final SimuComConfig simuComConfig;

	/*State representing current simulation run*/
	private final DefaultState halfDoneState;
	/*Snapshot taken from earlier simulation run*/
	private final Snapshot snapToInitOn;

	/* helper */
	private final Map<UsageModelPassedElement<?>, Double> event2offset;

	@Inject
	public SnapshotGraphStateBehaviour(final DefaultState halfDoneState, final Snapshot snapToInitOn,
			final SnapshotConfiguration snapshotConfig, final SimuComConfig simuComConfig) {
		this.halfDoneState = halfDoneState;
		this.snapToInitOn = snapToInitOn;
		this.snapshotConfig = snapshotConfig;
		this.simuComConfig = simuComConfig;

		this.event2offset = new HashMap<>();
	}

	/**
	 * Schedule {@link SnapshotInitiated} at maximum duration of the current
	 * simulation run.
	 *
	 * @param configurationStarted
	 * @return
	 */
	@Subscribe
	public Result<SnapshotInitiated> onConfigurationStarted(
			final PreSimulationConfigurationStarted configurationStarted) {
		return Result.of(new SnapshotInitiated(this.snapshotConfig.getSnapinterval()));
	}

	/**
	 *
	 * Start the simulation run with the snapshotted events from an earlier
	 * simulation run.
	 *
	 * Return (and thereby submit for scheduling the snapshotted events from
	 *
	 * @param simulationStarted
	 * @return
	 */
	@Subscribe
	public Result<DESEvent> onSimulationStarted(final SimulationStarted simulationStarted) {
		assert snapshotConfig.isStartFromSnapshot();

		final Set<DESEvent> initialEvents = this.snapToInitOn.getEvents();
		this.initOffsets(initialEvents);
		return Result.from(initialEvents);
	}

	/**
	 * Route the {@link SimulationStarted} event to either start from snapshotted
	 * events, or with an clean simulator. This is mutually exclusive, it's never
	 * both.
	 *
	 * TODO in case we get mutual exclusiveness for Subscribers as a Slingshot
	 * feature this operation will probably be obsolete.
	 *
	 * @param information interception information
	 * @param event       intercepted event
	 * @return
	 */
	@PreIntercept
	public InterceptionResult preInterceptSimulationStarted(final InterceptorInformation information,
			final SimulationStarted event) {


		if (information.getEnclosingType().isPresent() && ((snapshotConfig.isStartFromSnapshot() && information.getEnclosingType().get().equals(this.getClass()))
				|| (!snapshotConfig.isStartFromSnapshot() && !information.getEnclosingType().get().equals(this.getClass())))) {
			LOGGER.debug(String.format("Route %s to %s", event.getName(),
					information.getEnclosingType().get().getSimpleName()));
			return InterceptionResult.success();
		}

		return InterceptionResult.abort(); // won't be delivered.
	}

	/**
	 * Catch ModelPassedEvent from the snapshot and offset them into the past.
	 *
	 * @param information interception information
	 * @param event       intercepted event
	 * @return always success
	 */
	@PreIntercept
	public InterceptionResult preInterceptModelPassedEvent(final InterceptorInformation information,
			final UsageModelPassedElement<?> event) {
		if (event2offset.containsKey(event)) {
			final double offset = event2offset.remove(event);
			event.setTime(-offset);
			// adjust time for fakes - it's already past scheduling, thus no one really
			// cares.
		}
		return InterceptionResult.success();
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
	 * NB : state is complete.
	 *
	 * @param event
	 * @return
	 */
	@Subscribe
	public Result<SimulationFinished> onSnapshotFinished(final SnapshotFinished event) {
		halfDoneState.setSnapshot(event.getEntity());
		halfDoneState.setDuration(event.time());
		// Do not add the state anywhere, just finalise it. Assumption is, it already is
		// in the graph.
		return Result.of(new SimulationFinished());
	}

	/**
	 *
	 * Extract offset (encoded into the time field of the events) from the events
	 * into a map and set the event's time to 0.
	 *
	 * @param events events ot extract offset from.
	 */
	private void initOffsets(final Set<DESEvent> events) {
		events.stream().filter(event -> event instanceof UsageModelPassedElement<?>).forEach(event -> {
			event2offset.put((UsageModelPassedElement<?>) event, event.time());
			event.setTime(0);
		});
	}
}
