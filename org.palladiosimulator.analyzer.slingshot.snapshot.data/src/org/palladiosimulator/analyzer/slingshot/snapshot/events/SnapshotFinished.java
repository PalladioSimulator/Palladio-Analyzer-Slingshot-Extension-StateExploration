package org.palladiosimulator.analyzer.slingshot.snapshot.events;

import org.palladiosimulator.analyzer.slingshot.simulation.events.AbstractEntityChangedEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;

public class SnapshotFinished extends AbstractEntityChangedEvent<Snapshot>{
	
public SnapshotFinished(final Snapshot entity, final double delay) {
	super(entity, delay);
}

public SnapshotFinished(final Snapshot entity) {
	super(entity, 0);
} 
}