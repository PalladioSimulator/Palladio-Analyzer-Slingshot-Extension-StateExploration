package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateInitialized;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.behavior.usageevolution.events.IntervalPassed;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelChange;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ResourceEnvironmentChange;
import org.palladiosimulator.analyzer.slingshot.common.utils.events.ModelPassedEvent;
import org.palladiosimulator.analyzer.slingshot.core.events.PreSimulationConfigurationStarted;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.cost.events.TakeCostMeasurement;
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
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcmmeasuringpoint.ResourceContainerMeasuringPoint;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

/**
 *
 * Behavioural Extension to handle everything related to the RawGraphState.
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when = CalculatorRegistered.class, then = {})
@OnEvent(when = SimulationStarted.class, then = { AbstractEntityChangedEvent.class,
		SPDAdjustorStateInitialized.class }, cardinality = EventCardinality.MANY)
@OnEvent(when = SnapshotFinished.class, then = SimulationFinished.class)
@OnEvent(when = PreSimulationConfigurationStarted.class, then = SnapshotInitiated.class, cardinality = EventCardinality.SINGLE)
@OnEvent(when = ModelAdjusted.class, then = { TakeCostMeasurement.class }, cardinality = EventCardinality.MANY)
@OnEvent(when = TakeCostMeasurement.class, then = { TakeCostMeasurement.class }, cardinality = EventCardinality.MANY)
@OnEvent(when = SPDAdjustorStateInitialized.class, then = {})
@OnEvent(when = SnapshotInitiated.class, then = { TakeCostMeasurement.class }, cardinality = EventCardinality.MANY)
public class SnapshotGraphStateBehaviour implements SimulationBehaviorExtension {

	private final Logger LOGGER = Logger.getLogger(SnapshotGraphStateBehaviour.class);

	/* Configurations */
	private final SnapshotConfiguration snapshotConfig;
	private final SimuComConfig simuComConfig;

	/* State representing current simulation run */
	private final DefaultState halfDoneState;
	/* Snapshotted events taken from earlier simulation run */
	private final Set<DESEvent> eventsToInitOn;

	private final Map<String, SPDAdjustorStateValues> policyIdToValues;

	/* helper */
	private final Map<ModelPassedEvent<?>, Double> event2offset;

	private final boolean activated;

	private final Allocation allocation;

	/* for deleting monitors and MP of scaled in resources */
	private final MonitorRepository monitorrepo;
	private final MeasuringPointRepository measuringpointsrepo;

	@Inject
	public SnapshotGraphStateBehaviour(final @Nullable DefaultState halfDoneState,
			final @Nullable SnapshotConfiguration snapshotConfig, final @Nullable EventsToInitOnWrapper eventsToInitOn,
			final @Nullable SimuComConfig simuComConfig,
			final Allocation allocation, final MonitorRepository monitorrepo) {

		this.activated = halfDoneState != null && snapshotConfig != null && simuComConfig != null
				&& !monitorrepo.getMonitors().isEmpty();

		this.halfDoneState = halfDoneState;
		this.snapshotConfig = snapshotConfig;
		this.simuComConfig = simuComConfig;
		this.allocation = allocation;
		this.monitorrepo = monitorrepo;

		if (this.monitorrepo.getMonitors().isEmpty()) {
			this.measuringpointsrepo = null;
		} else {
			this.measuringpointsrepo = this.monitorrepo.getMonitors().get(0).getMeasuringPoint()
					.getMeasuringPointRepository();
		}

		if (activated) {
			assert halfDoneState.getSnapshot() == null : "Snapshot already set, but should not be!";
			this.eventsToInitOn = eventsToInitOn.getEventsToInitOn();
		} else {
			this.eventsToInitOn = Set.of();
		}

		this.event2offset = new HashMap<>();
		this.policyIdToValues = new HashMap<>();
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
		|| this.eventsToInitOn.stream().allMatch(
				e -> e instanceof ModelAdjustmentRequested || e instanceof SPDAdjustorStateInitialized)
		: "Received an SimulationStarted event, but is not configured to start from a snapshot.";

		this.initOffsets(this.eventsToInitOn);
		final Set<DESEvent> eventsToInitOnNoIntervallPassed = this.initIntervallPased(this.eventsToInitOn);

		return Result.of(eventsToInitOnNoIntervallPassed);
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

	Collection<TakeCostMeasurement> costMeasurementStore = new ArrayList<>();
	boolean handleCosts = true;

	/**
	 *
	 * Intercept {@link TakeCostMeasurement} events.
	 *
	 * For two reasons: Firstly, get to know all resources with cost measures to
	 * trigger a measurement in case of a snapshot. Secondly, abort the events, if
	 * the state starts with an adaptation. In this case, cost must only be measured
	 * after the adaptation, c.f.
	 * {@link SnapshotGraphStateBehaviour#onModelAdjusted(ModelAdjusted)}
	 *
	 *
	 * @param information
	 * @param event
	 * @return success, if this state starts without adaptation, abort other wise.
	 */
	@PreIntercept
	public InterceptionResult preInterceptTakeCostMeasurement(final InterceptorInformation information,
			final TakeCostMeasurement event) {

		if (handleCosts && event.time() == 0) {
			costMeasurementStore.add(event);

			if (this.eventsToInitOn.stream().filter(ModelAdjustmentRequested.class::isInstance).findAny()
					.isPresent()) {
				return InterceptionResult.abort();
			}
		}
		return InterceptionResult.success();

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
			final ModelPassedEvent<?> event) {
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
	 * TODO: why am i even including them in the snapshot to begin with?
	 *
	 * @param information interception information
	 * @param event       intercepted event
	 * @return always success
	 */
	//	@PreIntercept
	//	public InterceptionResult preInterceptIntervalPassed(final InterceptorInformation information,
	//			final IntervalPassed event) {
	//		if (resourceContainer2intervalPassed.isEmpty()) {
	//			return InterceptionResult.success();
	//		}
	//
	//		final String target = event.getTargetResourceContainer().getId();
	//
	//		if (resourceContainer2intervalPassed.containsKey(target)) {
	//			resourceContainer2intervalPassed.remove(target);
	//			return InterceptionResult.abort();
	//		}
	//
	//		return InterceptionResult.success();
	//	}

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
	 * Catch {@link TakeCostMeasurement} events (usage evolution) from the snapshot and
	 * offset them into the "future". Otherwise, we will get the wrong values from
	 * the Load Intensity model.
	 *
	 * @param information interception information
	 * @param event       intercepted event
	 * @return always success
	 */
	@PreIntercept
	public InterceptionResult preInterceptIntervalPassed(final InterceptorInformation information,
			final IntervalPassed event) {
		event.setTime(event.time() + halfDoneState.getStartTime());
		return InterceptionResult.success();
	}

	@Subscribe
	public Result<TakeCostMeasurement> onSnapshotInitiated(final SnapshotInitiated event) {
		return Result.of(costMeasurementStore);
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

		halfDoneState.addAdjustorStateValues(
				this.policyIdToValues.values().stream().map(s -> this.setOffsets(s, event.time())).toList());

		// Do not add the state anywhere, just finalise it. Assumption is, it already is
		// in the graph.
		return Result.of(new SimulationFinished());
	}

	/**
	 * Update persisted model files, because reconfiguration now happens at
	 * runtime,https://chat.rss.iste.uni-stuttgart.de/group/Floriments-doctor-hat
	 * i.e. not yet propagated to file.
	 *
	 * @param modelAdjusted
	 */
	@Subscribe
	public Result<TakeCostMeasurement> onModelAdjusted(final ModelAdjusted modelAdjusted) {
		for (final ModelChange<?> change : modelAdjusted.getChanges()) {
			if (change instanceof final ResourceEnvironmentChange resEnvChange) {
				for (final ResourceContainer container : resEnvChange.getDeletedResourceContainers()) {
					removeDeletedMonitoring(container);
				}
			}
		}
		ArchitectureConfigurationUtil.saveWhitelisted(this.allocation.eResource().getResourceSet());
		this.handleCosts = false;

		return Result.of(costMeasurementStore);
	}

	/**
	 * Remove Monitors and Measuringpoints that reference the
	 * {@link ResourceContainer} that was deleted during a scale in.
	 *
	 * @param deleted {@link ResourceContainer} deleted during scale in.
	 */
	private void removeDeletedMonitoring(final ResourceContainer deleted) {

		final Set<MeasuringPoint> deletedMps = new HashSet<>();

		for (final MeasuringPoint mp : Set.copyOf(measuringpointsrepo.getMeasuringPoints())) {
			if (mp instanceof final ResourceContainerMeasuringPoint rcmp
					&& rcmp.getResourceContainer().getId().equals(deleted.getId())) {
				deletedMps.add(rcmp);
				measuringpointsrepo.getMeasuringPoints().remove(rcmp);
			}
		}

		for (final Monitor monitor : Set.copyOf(monitorrepo.getMonitors())) {
			if (deletedMps.contains(monitor.getMeasuringPoint())) {
				monitorrepo.getMonitors().remove(monitor);
			}
		}
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
	 * Extract offset (encoded into the time field of the events) from the events
	 * into a map and set the event's time to 0.
	 *
	 * @param events events to extract offset from.
	 */
	private void initOffsets(final Set<DESEvent> events) {
		events.stream().filter(event -> event instanceof ModelPassedEvent<?>).forEach(event -> {
			event2offset.put((ModelPassedEvent<?>) event, event.time());
			event.setTime(0);
		});
	}

	/**
	 * Collect resource containers, for which an {@link TakeCostMeasurement} event must
	 * be aborted later on.
	 *
	 * @param events events to collect resource containers from.
	 */
	private Set<DESEvent> initIntervallPased(final Set<DESEvent> events) {
		// events.stream().filter(event -> event instanceof IntervalPassed)
		// .forEach(event -> resourceContainer2intervalPassed
		// .put(((IntervalPassed) event).getTargetResourceContainer().getId(),
		// (IntervalPassed) event));

		return events.stream().filter(Predicate.not(TakeCostMeasurement.class::isInstance)).collect(Collectors.toSet());
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

		return new SPDAdjustorStateValues(stateValues.scalingPolicyId(), latestAdjustmentAtSimulationTime, numberScales,
				coolDownEnd, numberOfScalesInCooldown);
	}
}
