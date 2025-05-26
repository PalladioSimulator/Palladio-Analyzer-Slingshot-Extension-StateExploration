package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashMap;
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
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
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

	/* for displacing modelpassed events to the past */
	private final Map<ModelPassedEvent<?>, Double> offsetMap;

	private final EventsToInitOnWrapper wrapper;

	private final SimulationScheduling scheduling;

	@Inject
	public SnapshotInitFromBehaviour(final @Nullable SnapshotConfiguration snapshotConfig,
			final @Nullable EventsToInitOnWrapper eventsWrapper, final SimulationScheduling scheduling) {
		this.snapshotConfig = snapshotConfig;
		this.wrapper = eventsWrapper;
		this.scheduling = scheduling;

		this.offsetMap = new HashMap<>();
	}

	@Override
	public boolean isActive() {
		return this.snapshotConfig != null && this.wrapper != null;
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
		return Result.of(new SnapshotInitiated(this.snapshotConfig.getMinDuration()));
	}

	/**
	 *
	 * Start the simulation run with the snapshotted events from an earlier
	 * simulation run.
	 *
	 * Schedules the snapshotted events from earlier simulation run directly to the
	 * engine to preserve order.
	 *
	 * @param simulationStarted
	 */
	@Subscribe
	public void onSimulationStarted(final SimulationStarted simulationStarted) {
		// schedule one event after the other directly to the engine to preserver order.
		wrapper.getAdjustmentEvents().forEach(e -> scheduling.scheduleEvent(e));

		final Set<DESEvent> eventsToInitOn = this.removeTakeCostMeasurement(this.wrapper.getOtherEvents());
		this.initOffsetMap(eventsToInitOn);

		eventsToInitOn.forEach(e -> scheduling.scheduleEvent(e));
		wrapper.getStateInitEvents().forEach(e -> scheduling.scheduleEvent(e));
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
		if (information.getEnclosingType().get().equals(this.getClass())) {
			return InterceptionResult.success();
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
		if (offsetMap.containsKey(event)) {
			final double offset = offsetMap.remove(event);
			event.setTime(-offset);
			// adjust time for fakes - it's already past scheduling, thus scheduling order is not affeced.
		}
		return InterceptionResult.success();
	}

	/**
	 *
	 * Extract offset (encoded into the time field of the events) from the events
	 * into a map and set the event's time to 0 for immediate scheduling.
	 *
	 * @param events events to extract offsets from.
	 */
	private void initOffsetMap(final Set<DESEvent> events) {
		events.stream().filter(event -> event instanceof ModelPassedEvent<?>).forEach(event -> {
			offsetMap.put((ModelPassedEvent<?>) event, event.time());
			event.setTime(0);
		});
	}

	/**
	 * Remove all {@link TakeCostMeasurement} events from the given set.
	 * 
	 * {@link TakeCostMeasurement} events must be removed, because we need cost
	 * measurements at t = 0 of each state.
	 *
	 * @param events set of events to be cleansed.
	 * @return set of events without {@link TakeCostMeasurement}
	 */
	private Set<DESEvent> removeTakeCostMeasurement(final Set<DESEvent> events) {
		return events.stream().filter(Predicate.not(TakeCostMeasurement.class::isInstance)).collect(Collectors.toSet());
	}
}
