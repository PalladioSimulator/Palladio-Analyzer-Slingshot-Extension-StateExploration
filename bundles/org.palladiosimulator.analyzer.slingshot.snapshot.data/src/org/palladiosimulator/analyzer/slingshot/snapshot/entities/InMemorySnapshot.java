package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
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

	private final List<ModelAdjustmentRequested> modelAdjustmentRequestedEvents;
	
	private final Collection<SPDAdjustorStateValues> adjustorStateValues;

	public InMemorySnapshot() {
		this(Set.of());

	}

	public InMemorySnapshot(final Set<DESEvent> events) {
		this.events = events;
		this.modelAdjustmentRequestedEvents = new ArrayList<>();
		this.adjustorStateValues = new ArrayList<>();
	}


	@Override
	public Set<DESEvent> getEvents(final PCMResourceSetPartition set) {
		final CloneHelperWithVisitor cloneHelper = new CloneHelperWithVisitor(set);
		return cloneHelper.clone(this.events);
	}

	@Override
	public List<ModelAdjustmentRequested> getModelAdjustmentRequestedEvent() {
		return this.modelAdjustmentRequestedEvents;
	}

	@Override
	public void addModelAdjustmentRequestedEvent(final ModelAdjustmentRequested event) {
		this.modelAdjustmentRequestedEvents.add(event);
	}

	@Override
	public Collection<SPDAdjustorStateValues> getSPDAdjustorStateValues() {
		return this.adjustorStateValues;
	}

	@Override
	public void addSPDAdjustorStateValues(final SPDAdjustorStateValues value) {
		this.adjustorStateValues.add(value);
		
	}
}