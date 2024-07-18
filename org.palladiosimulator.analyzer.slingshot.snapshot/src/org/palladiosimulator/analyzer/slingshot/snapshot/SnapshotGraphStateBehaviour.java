package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.utils.events.ModelPassedEvent;
import org.palladiosimulator.analyzer.slingshot.core.events.PreSimulationConfigurationStarted;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.cost.events.IntervalPassed;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.CalculatorRegistered;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfigurationUtil;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.Repository.Repository;
import org.palladiosimulator.pcm.allocation.Allocation;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

/**
 *
 * Behavioural Extension to handle everything related to the RawGraphState.
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when = CalculatorRegistered.class, then = {})
@OnEvent(when = SimulationStarted.class, then = AbstractEntityChangedEvent.class, cardinality = EventCardinality.MANY)
@OnEvent(when = SnapshotFinished.class, then = SimulationFinished.class)
@OnEvent(when = PreSimulationConfigurationStarted.class, then = SnapshotInitiated.class)
@OnEvent(when = ModelAdjusted.class, then = {})
public class SnapshotGraphStateBehaviour implements SimulationBehaviorExtension {

	private final Logger LOGGER = Logger.getLogger(SnapshotGraphStateBehaviour.class);

	/* Configurations */
	private final SnapshotConfiguration snapshotConfig;
	private final SimuComConfig simuComConfig;

	/* State representing current simulation run */
	private final DefaultState halfDoneState;
	/* Snapshotted events taken from earlier simulation run */
	private final Set<DESEvent> eventsToInitOn;

	/* helper */
	private final Map<UsageModelPassedElement<?>, Double> event2offset;
	private final Map<String, IntervalPassed> resourceContainer2intervalPassed;

	private final boolean activated;

	private final Allocation allocation;

	@Inject
	public SnapshotGraphStateBehaviour(final @Nullable DefaultState halfDoneState,
			final @Nullable SnapshotConfiguration snapshotConfig, final @Nullable EventsToInitOnWrapper eventsToInitOn,
			final @Nullable SimuComConfig simuComConfig,
			final Allocation allocation) {

		this.activated = halfDoneState != null && snapshotConfig != null && simuComConfig != null;

		this.halfDoneState = halfDoneState;
		this.snapshotConfig = snapshotConfig;
		this.simuComConfig = simuComConfig;
		this.allocation = allocation;


		if (activated) {
			assert halfDoneState.getSnapshot() == null : "Snapshot already set, but should not be!";
			this.eventsToInitOn = eventsToInitOn.getEventsToInitOn();
		} else {
			this.eventsToInitOn = Set.of();
		}

		this.event2offset = new HashMap<>();
		this.resourceContainer2intervalPassed = new HashMap<>();
	}

	@Override
	public boolean isActive() {
		return this.activated;
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
	 * Return (and thereby submit for scheduling) the snapshotted events from
	 * earlier simulation run.
	 *
	 * @param simulationStarted
	 * @return
	 */
	@Subscribe
	public Result<DESEvent> onSimulationStarted(final SimulationStarted simulationStarted) {
		assert snapshotConfig.isStartFromSnapshot()
		|| this.eventsToInitOn.stream().allMatch(ModelAdjustmentRequested.class::isInstance)
		: "Received an SimulationStarted event, but is not configured to start from a snapshot.";

		this.initOffsets(this.eventsToInitOn);
		this.initIntervallPased(this.eventsToInitOn);
		return Result.of(this.eventsToInitOn);
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

		if (!information.getEnclosingType().isPresent()) { // should not happen, right?
			return InterceptionResult.abort(); // won't be delivered.
		}

		if (information.getEnclosingType().get().equals(this.getClass())) {
			// delievering to this class
			if (!this.eventsToInitOn.isEmpty()) {
				return InterceptionResult.success();
			} else {
				return InterceptionResult.abort(); // won't be delivered.
			}
		} else {
			// delivering e.g. to another class.
			if (snapshotConfig.isStartFromSnapshot()) {
				return InterceptionResult.abort();
			} else {
				return InterceptionResult.success(); // won't be delivered.
			}
		}
	}

	/**
	 * Catch {@link ModelPassedEvent} from the snapshot and offset them into the
	 * past.
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
	 * Catch {@link IntervalPassed} (cost) from the snapshot abort them, because for
	 * costs, we use the events of the current simulation run.
	 *
	 * @param information interception information
	 * @param event       intercepted event
	 * @return always success
	 */
	@PreIntercept
	public InterceptionResult preInterceptIntervalPassed(final InterceptorInformation information,
			final IntervalPassed event) {
		if (resourceContainer2intervalPassed.isEmpty()) {
			return InterceptionResult.success();
		}

		final String target = event.getTargetResourceContainer().getId();

		if (resourceContainer2intervalPassed.containsKey(target)) {
			resourceContainer2intervalPassed.remove(target);
			return InterceptionResult.abort();
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
	 * Catch {@link IntervalPassed} events (usage evolution) from the snapshot and
	 * offset them into the "future". Otherwise, we will get the wrong values from
	 * the Load Intensity model.
	 *
	 * @param information interception information
	 * @param event       intercepted event
	 * @return always success
	 */
	@PreIntercept
	public InterceptionResult preInterceptIntervalPassed(final InterceptorInformation information,
			final org.palladiosimulator.analyzer.slingshot.behavior.usageevolution.events.IntervalPassed event) {
		event.setTime(event.time() + halfDoneState.getStartTime());
		return InterceptionResult.success();
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
	 * Update persisted model files, because reconfiguration now happens at runtime,
	 * i.e. not yet propagated to file.
	 *
	 * @param modelAdjusted
	 */
	@Subscribe
	public void onModelAdjusted(final ModelAdjusted modelAdjusted) {
		ArchitectureConfigurationUtil.saveWhitelisted(this.allocation.eResource().getResourceSet());
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

	/**
	 * Collect resource containers, for which an {@link IntervalPassed} event must
	 * be aborted later on.
	 *
	 * @param events events to collect resource containers from.
	 */
	private void initIntervallPased(final Set<DESEvent> events) {
		events.stream().filter(event -> event instanceof IntervalPassed)
		.forEach(event -> resourceContainer2intervalPassed
				.put(((IntervalPassed) event).getTargetResourceContainer().getId(), (IntervalPassed) event));
	}
}
