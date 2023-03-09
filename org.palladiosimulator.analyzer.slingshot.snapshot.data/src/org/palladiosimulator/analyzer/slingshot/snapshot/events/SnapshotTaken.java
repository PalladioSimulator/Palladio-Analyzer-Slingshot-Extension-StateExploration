package org.palladiosimulator.analyzer.slingshot.snapshot.events;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

/**
 *
 *
 * @author stiesssh
 *
 */
public class SnapshotTaken extends AbstractSimulationEvent {
	public SnapshotTaken() {
		this(0);
	}

	public SnapshotTaken(final double delay) {
		super("TODO", delay);
	}
}
