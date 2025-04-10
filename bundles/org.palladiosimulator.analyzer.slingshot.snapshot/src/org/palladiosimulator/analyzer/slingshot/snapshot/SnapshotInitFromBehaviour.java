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
 * TODO initialization stuff
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = SimulationStarted.class, then = { AbstractEntityChangedEvent.class,
		SPDAdjustorStateInitialized.class }, cardinality = EventCardinality.MANY)
@OnEvent(when = PreSimulationConfigurationStarted.class, then = SnapshotInitiated.class, cardinality = EventCardinality.SINGLE)
public class SnapshotInitFromBehaviour implements SimulationBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(SnapshotInitFromBehaviour.class);

	/* Configurations */
	private final SnapshotConfiguration snapshotConfig;

	/* Snapshotted events taken from earlier simulation run */
	private final Set<DESEvent> eventsToInitOn;

	/* helper */
	private final Map<ModelPassedEvent<?>, Double> event2offset;

	private final boolean activated;

	private final EventsToInitOnWrapper wrapper;

	@Inject
	public SnapshotInitFromBehaviour(final @Nullable SnapshotConfiguration snapshotConfig, final @Nullable EventsToInitOnWrapper eventsWrapper) {

		this.activated = snapshotConfig != null;

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
	 * If there are any initialisation events, the event is always routed to this
	 * class.
	 * 
	 * If the simulation starts from a snapshot, the event is aborted to all other
	 * simulator classes. Otherwise, event delivered to the other classes and the
	 * simulation starts normally.
	 *
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
