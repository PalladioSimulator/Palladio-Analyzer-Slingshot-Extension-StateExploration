package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateInitialized;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.utils.events.ModelPassedEvent;
import org.palladiosimulator.analyzer.slingshot.core.events.PreSimulationConfigurationStarted;
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
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;

/**
 *
 * This Class is all about the events needed for initialising a new simulation
 * run based on a previously snapshotted simulation run.
 * 
 * This encompasses:
 * <li>routing the {@link SimulationStarted} event, such that this simulation
 * start off with one set request only. Otherwise, the simulation run would
 * start with the requests from the snapshot but also create additional
 * requests, as it would at the beginning of a 'normal' simulation.
 * <li>re-publish events that used to be scheduled, i.e. events from the future
 * event list of the previous simulation run.
 * <li>publish {@link ModelAdjustmentRequested} event(s) if pro- or reactive
 * reconfiguration are planned for this simulation run.
 * <li>publish additional events to set the state of the statefull components of
 * the simulator.
 * <li>publish the {@link SnapshotInitiated} event, to mark the end of this
 * simulation run (unless it ends earlier for other reasons.)
 * <li>offset all {@link ModelPassedEvent}s that are leftover from the previous
 * state into the past.
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = SimulationStarted.class, then = { AbstractEntityChangedEvent.class,
		SPDAdjustorStateInitialized.class }, cardinality = EventCardinality.MANY)
@OnEvent(when = PreSimulationConfigurationStarted.class, then = SnapshotInitiated.class, cardinality = EventCardinality.SINGLE)
public class SnapshotInitFromBehaviour implements SimulationBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(SnapshotInitFromBehaviour.class);

	private final SnapshotConfiguration snapshotConfig;

	/** Snapshotted events taken from earlier simulation run */
	private final Set<DESEvent> eventsToInitOn;

	/* helper */
	private final Map<ModelPassedEvent<?>, Double> event2offset;

	private final boolean activated;

	private final EventsToInitOnWrapper wrapper;

	@Inject
	public SnapshotInitFromBehaviour(final @Nullable SnapshotConfiguration snapshotConfig,
			final @Nullable EventsToInitOnWrapper eventsWrapper) {

		this.activated = snapshotConfig != null && eventsWrapper != null;

		this.snapshotConfig = snapshotConfig;

		this.wrapper = eventsWrapper;

		if (activated) {
			this.eventsToInitOn = eventsWrapper.getOtherEvents();
		} else {
			this.eventsToInitOn = Set.of();
		}

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
	 * @return {@link SnapshotInitiated} event indicating the lates point in time
	 *         for a snapshot.
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
	 * TODO changes this, because the result mixes up the event order.
	 *
	 * @param simulationStarted
	 * @return
	 */
	@Subscribe
	public Result<DESEvent> onSimulationStarted(final SimulationStarted simulationStarted) {
		assert snapshotConfig.isStartFromSnapshot()
				|| (this.eventsToInitOn.isEmpty() && !wrapper.getAdjustmentEvents().isEmpty())
				: "Received an SimulationStarted event, but is not configured to start from a snapshot.";

		this.initOffsets(this.eventsToInitOn);
		final Set<DESEvent> eventsToInitOnNoIntervallPassed = this.removeTakeCostMeasurement(this.eventsToInitOn);

		final List<DESEvent> allEvents = new ArrayList<>();
		allEvents.addAll(wrapper.getStateInitEvents());
		allEvents.addAll(wrapper.getAdjustmentEvents());
		allEvents.addAll(eventsToInitOnNoIntervallPassed);

		LOGGER.info("Initialise on "
				+ wrapper.getAdjustmentEvents().stream().map(e -> e.getScalingPolicy().getEntityName()).toList());

		return Result.of(allEvents);
	}

	/**
	 * Route a {@link SimulationStarted} event.
	 * 
	 * If there are any events to be published for initialising the simulator, the
	 * event is always routed to this class.
	 * 
	 * If the simulation starts from a snapshot, the {@link SimulationStarted} event
	 * is aborted to all other simulator classes. Otherwise, the
	 * {@link SimulationStarted} event is also delivered to the other classes and
	 * the simulation starts normally.
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

		// TODO : actually, i think we need not abort forwarding the simulation started
		// to this class.
		if (information.getEnclosingType().get().equals(this.getClass())) {
			// delievering to this class, if there is any event for initialisation.
			if (!this.eventsToInitOn.isEmpty() || !this.wrapper.getAdjustmentEvents().isEmpty()
					|| !this.wrapper.getStateInitEvents().isEmpty()) {
				return InterceptionResult.success();
			} else {
				return InterceptionResult.abort();
			}
		} else {
			if (snapshotConfig.isStartFromSnapshot()) {
				return InterceptionResult.abort();
			} else {
				return InterceptionResult.success();
			}
		}
	}

	/**
	 * Catch {@link ModelPassedEvent} from the snapshot and offset them into the
	 * past.
	 * 
	 * This is necessary, because durations, such as response time, are calculated
	 * based on {@link ModelPassedEvent} events.
	 * 
	 * Example: A request enters the system at t = 5 and leaves at t = 15. However,
	 * at t = 10, a snapshot is taken, and a new simulation run is started.
	 * 
	 * The {@link ModelPassedEvent} event, that indicated the request entering the
	 * system at t = 5 is part of the snapshot and is published at the beginning of
	 * the new simulation run. However, the re-published event, now has time t = 10,
	 * wrt. the entire exploration, respectively t = 0, wrt. the new simulation run.
	 * Thus the event must be offsetted into the past by 5s to get the correct
	 * response time.
	 * 
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
	 * Remove all {@link TakeCostMeasurement} from the given set.
	 *
	 * @param events set of events to be cleansed.
	 * 
	 * @return set of events without {@link TakeCostMeasurement}
	 */
	private Set<DESEvent> removeTakeCostMeasurement(final Set<DESEvent> events) {
		return events.stream().filter(Predicate.not(TakeCostMeasurement.class::isInstance)).collect(Collectors.toSet());
	}
}
