package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelperWithVisitor;
import org.palladiosimulator.analyzer.slingshot.simulation.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;

/**
 *
 * @author stiesssh
 *
 */
public final class InMemorySnapshot implements Snapshot {

	private final Set<DESEvent> events;

	public InMemorySnapshot() {
		this.events = Set.of();
	}
	
	public InMemorySnapshot(final Set<DESEvent> events) {
		this.events = events;
	}

	@Override
	public Set<DESEvent> getEvents() {
		final CloneHelperWithVisitor cloneHelper = new CloneHelperWithVisitor();
		return cloneHelper.clone(this.events);
	}
}
