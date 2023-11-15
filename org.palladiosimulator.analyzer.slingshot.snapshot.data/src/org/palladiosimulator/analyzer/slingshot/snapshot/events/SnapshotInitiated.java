package org.palladiosimulator.analyzer.slingshot.snapshot.events;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.AdjustorBasedEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

/**
 *
 * inform subscribers, that a snapshot will be taken.
 * more or less a helper, such that the resource simulation may update the resources internal timer in preparation of the snap shot.
 *
 * maybe i can remove it, once we get the new framework with the preinterception annotation.
 *
 * @author stiesssh
 *
 */
public class SnapshotInitiated extends AbstractSimulationEvent{

	private final Optional<AdjustorBasedEvent> triggeringEvent;

	public SnapshotInitiated() {
		this(0);
	}

	public SnapshotInitiated(final double delay) {
		super("TODO", delay);
		this.triggeringEvent = Optional.empty();
	}

	public SnapshotInitiated(final double delay, final AdjustorBasedEvent event) {
		super("TODO", delay);
		this.triggeringEvent = Optional.of(event);
	}

	public Optional<AdjustorBasedEvent> getTriggeringEvent() {
		return triggeringEvent;
	}
}
