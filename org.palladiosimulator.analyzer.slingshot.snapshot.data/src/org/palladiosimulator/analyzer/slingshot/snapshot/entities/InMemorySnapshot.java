package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.Optional;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelperWithVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;

/**
 *
 * @author stiesssh
 *
 */
public final class InMemorySnapshot implements Snapshot {

	private final Set<DESEvent> events;

	private Optional<DESEvent> adjustorEvent;

	public InMemorySnapshot() {
		this(Set.of());

	}

	public InMemorySnapshot(final Set<DESEvent> events) {
		this.events = events;
		this.adjustorEvent = Optional.empty();
	}


	@Override
	public Set<DESEvent> getEvents() {
		final CloneHelperWithVisitor cloneHelper = new CloneHelperWithVisitor();
		return cloneHelper.clone(this.events);
	}

	@Override
	public Optional<DESEvent> getAdjustorEvent() {
		return this.adjustorEvent;
	}

	@Override
	public void setAdjustorEvent(final DESEvent event) {
		this.adjustorEvent = Optional.of(event);
	}
}
