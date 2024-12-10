package org.palladiosimulator.analyzer.slingshot.snapshot.events;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

/**
 *
 * inform subscribers, that a snapshot will be taken. more or less a helper,
 * such that the resource simulation may update the resources internal timer in
 * preparation of the snap shot.
 *
 * maybe i can remove it, once we get the new framework with the preinterception
 * annotation.
 *
 * Reminder : {@link ModelAdjustmentRequested} gets published after the spd
 * interpreter checked all triggers and constraints, and decided that a
 * reconfiguration should be executed. i.e. models have not yet changed.
 *
 * @author Sarah Stieß
 *
 */
public class SnapshotInitiated extends AbstractSimulationEvent {

	private final Optional<ModelAdjustmentRequested> triggeringEvent;

	public SnapshotInitiated(final double delay) {
		super("TODO", delay);
		this.triggeringEvent = Optional.empty();
	}

	public SnapshotInitiated(final double delay, final ModelAdjustmentRequested event) {
		super("TODO", delay);
		this.triggeringEvent = Optional.of(event);
	}

	public Optional<ModelAdjustmentRequested> getTriggeringEvent() {
		return triggeringEvent;
	}
}
