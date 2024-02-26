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
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
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
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.Repository.Repository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

/**
 *
 * Behavioural Extension to handle everything related to the RawGraphState.
 *
 * @author stiesssh
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

	private final boolean activated;

	private final Allocation allocation;
	private final MonitorRepository monitoring;

	@Inject
	public SnapshotGraphStateBehaviour(final @Nullable DefaultState halfDoneState,
			final @Nullable SnapshotConfiguration snapshotConfig, final @Nullable SimuComConfig simuComConfig,
			final @Nullable Set<DESEvent> eventsToInitOn, final Allocation allocation,
			final MonitorRepository monitoring) {
		this.halfDoneState = halfDoneState;
		this.snapshotConfig = snapshotConfig;
		this.simuComConfig = simuComConfig;
		this.eventsToInitOn = eventsToInitOn;
		this.allocation = allocation;
		this.monitoring = monitoring;

		this.activated = this.halfDoneState != null && this.snapshotConfig != null && this.simuComConfig != null
				&& eventsToInitOn != null;

		this.event2offset = new HashMap<>();
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
	 * Return (and thereby submit for scheduling the snapshotted events from
	 *
	 * @param simulationStarted
	 * @return
	 */
	@Subscribe
	public Result<DESEvent> onSimulationStarted(final SimulationStarted simulationStarted) {
		assert snapshotConfig.isStartFromSnapshot();

		this.initOffsets(this.eventsToInitOn);
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

		if (information.getEnclosingType().isPresent() && ((snapshotConfig.isStartFromSnapshot()
				&& information.getEnclosingType().get().equals(this.getClass()))
				|| (!snapshotConfig.isStartFromSnapshot()
						&& !information.getEnclosingType().get().equals(this.getClass())))) {
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
	 * Save state to file, because reconfiguration now happens at runtime, i.e. not
	 * yet propagated to file.
	 *
	 * @param modelAdjusted
	 */
	@Subscribe
	public void onModelAdjusted(final ModelAdjusted modelAdjusted) {
		// beware: das hier sind andere modelle in der archconfig, als die, die provided
		// werden!!
		// weil ich die instanzen aus der archconfig raus nehmen musste um sie in die
		// provider rein zu kriegen.

		ResourceUtils.saveResource(this.monitoring.eResource());

		if (!this.monitoring.getMonitors().isEmpty()) {
			LOGGER.debug("monitors defined, updating Meassuring points");
			MeasuringPointRepository mpRepo = this.monitoring.getMonitors().get(0).getMeasuringPoint()
					.getMeasuringPointRepository();
			ResourceUtils.saveResource(mpRepo.eResource());
		}

		ResourceUtils.saveResource(this.allocation.eResource());
		ResourceUtils.saveResource(this.allocation.getTargetResourceEnvironment_Allocation().eResource());
		ResourceUtils.saveResource(this.allocation.getSystem_Allocation().eResource());
		ResourceUtils.saveResource(this.allocation.getSystem_Allocation().getAssemblyContexts__ComposedStructure()
				.get(0).getEncapsulatedComponent__AssemblyContext().getRepository__RepositoryComponent().eResource());

		// Do NOT save the ScalingPolicyies, cause that would get the oneTrickPony into
		// the persisted Rules.

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
