package org.palladiosimulator.analyzer.slingshot.snapshot.events;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

/**
 *
 *
 * @author stiesssh
 *
 */
public class SnapshotTaken extends AbstractSimulationEvent {

	private final Optional<ModelAdjustmentRequested> triggeringEvent;

	public SnapshotTaken(final double delay, final Optional<ModelAdjustmentRequested> event) {
		super("TODO", delay);
		this.triggeringEvent = event;
	}

	public Optional<ModelAdjustmentRequested> getTriggeringEvent() {
		return triggeringEvent;
	}
}
