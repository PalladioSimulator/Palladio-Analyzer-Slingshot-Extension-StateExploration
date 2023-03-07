package org.palladiosimulator.analyzer.slingshot.snapshot.events;

import org.palladiosimulator.analyzer.slingshot.simulation.events.AbstractEvent;

/**
 * 
 * 
 * @author stiesssh
 *
 */
public class SnapshotTaken extends AbstractEvent {
	public SnapshotTaken() {
		this(0);
	}

	public SnapshotTaken(final double delay) {
		super(SnapshotTaken.class, delay);
	}
}
