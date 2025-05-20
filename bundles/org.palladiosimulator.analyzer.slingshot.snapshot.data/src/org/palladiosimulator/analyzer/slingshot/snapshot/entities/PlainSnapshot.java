package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

/**
 *
 * Snaphshot of a Simulation, with out any additional duplication etc.
 * 
 * As most {@link DESEvent}s are mutable, proceed with caution, if you want use
 * this snapshot to initialise a simulation run.
 *
 * @author Sophie Stieß
 *
 */
public final class PlainSnapshot implements Snapshot {

	private final Set<DESEvent> events;

	private final Set<DESEvent> plainEvents;

	private final List<ModelAdjustmentRequested> modelAdjustmentRequestedEvents;
	
	private final Collection<SPDAdjustorStateValues> adjustorStateValues;

	public PlainSnapshot(final Set<DESEvent> events, final Collection<SPDAdjustorStateValues> stateValues) {
		this.plainEvents = new HashSet<>(events);
		this.modelAdjustmentRequestedEvents = events.stream().filter(ModelAdjustmentRequested.class::isInstance).map(ModelAdjustmentRequested.class::cast).toList();
		this.events = new HashSet<>(events);
		this.events.removeAll(this.modelAdjustmentRequestedEvents);
		this.adjustorStateValues = stateValues;
	}


	@Override
	public Set<DESEvent> getEvents(final PCMResourceSetPartition set) {
		return Set.copyOf(this.events);
	}

	@Override
	public List<ModelAdjustmentRequested> getModelAdjustmentRequestedEvent() {
		return List.copyOf(this.modelAdjustmentRequestedEvents);
	}

	@Override
	public Collection<SPDAdjustorStateValues> getSPDAdjustorStateValues() {
		return Set.copyOf(this.adjustorStateValues);
	}

	@Override
	public Set<DESEvent> getPlainEvents() {
		return Set.copyOf(this.plainEvents);
	}
}