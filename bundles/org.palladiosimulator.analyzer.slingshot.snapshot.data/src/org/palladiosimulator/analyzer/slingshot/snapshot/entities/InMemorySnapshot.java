package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.Collection;
import java.util.HashSet;
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
 * @author Sophie Stieß
 *
 */
public final class InMemorySnapshot implements Snapshot {

	private final Set<DESEvent> events;

	private final Set<DESEvent> plainEvents;

	private final List<ModelAdjustmentRequested> modelAdjustmentRequestedEvents;
	
	private final Collection<SPDAdjustorStateValues> adjustorStateValues;

	public InMemorySnapshot() {
		this(Set.of(), Set.of());
	}

	public InMemorySnapshot(final Set<DESEvent> events, final Collection<SPDAdjustorStateValues> stateValues) {
		this.plainEvents = new HashSet<>(events);
		this.modelAdjustmentRequestedEvents = events.stream().filter(ModelAdjustmentRequested.class::isInstance).map(ModelAdjustmentRequested.class::cast).toList();
		this.events = new HashSet<>(events);
		this.events.removeAll(this.modelAdjustmentRequestedEvents);
		this.adjustorStateValues = stateValues;
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
	public Collection<SPDAdjustorStateValues> getSPDAdjustorStateValues() {
		return this.adjustorStateValues;
	}

	@Override
	public Set<DESEvent> getPlainEvents() {
		return this.plainEvents;
	}
}