package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.Optional;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
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

	private Optional<ModelAdjustmentRequested> adjustorEvent;

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
	public Optional<ModelAdjustmentRequested> getAdjustorEvent() {
		return this.adjustorEvent;
	}

	@Override
	public void setAdjustorEvent(final ModelAdjustmentRequested event) {
		this.adjustorEvent = Optional.of(event);
	}
}
