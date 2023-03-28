package org.palladiosimulator.analyzer.slingshot.snapshot.events;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.AdjustorBasedEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

/**
 *
 *
 * @author stiesssh
 *
 */
public class SnapshotTaken extends AbstractSimulationEvent {

	private final Optional<AdjustorBasedEvent> triggeringEvent;

	public SnapshotTaken(final double delay, final Optional<AdjustorBasedEvent> event) {
		super("TODO", delay);
		this.triggeringEvent = event;
	}

	public Optional<AdjustorBasedEvent> getTriggeringEvent() {
		return triggeringEvent;
	}
}
