package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.Optional;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelperWithVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

/**
 *
 * Snaphshot of a Simulation run that holds all information in memory.
 * 
 * @author Sarah Stieß
 *
 */
public final class InMemorySnapshot implements Snapshot {

	private final Set<DESEvent> events;

	private Optional<ModelAdjustmentRequested> modelAdjustmentRequestedEvent;

	public InMemorySnapshot() {
		this(Set.of());

	}

	public InMemorySnapshot(final Set<DESEvent> events) {
		this.events = events;
		this.modelAdjustmentRequestedEvent = Optional.empty();
	}


	@Override
	public Set<DESEvent> getEvents(final PCMResourceSetPartition set) {
		final CloneHelperWithVisitor cloneHelper = new CloneHelperWithVisitor(set);
		return cloneHelper.clone(this.events);
	}

	@Override
	public Optional<ModelAdjustmentRequested> getModelAdjustmentRequestedEvent() {
		return this.modelAdjustmentRequestedEvent;
	}

	@Override
	public void setModelAdjustmentRequestedEvent(final ModelAdjustmentRequested event) {
		this.modelAdjustmentRequestedEvent = Optional.of(event);
	}
}