package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelperWithVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Camera;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.EventRecord;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

/**
 * 
 * This class creates a {@link Snapshot} based on a the record of a {@link EventRecord} and the future events from the {@link SimulationEngine}.
 * 
 * All events in the snapshot are copies of the originals. 
 * Also, delays are already adjusted and offsets for resending those events are encoded into {@link DESEvent#time}.
 * 
 * 
 * @author Sophie Stieß
 *
 */
public final class LessInvasiveInMemoryCamera extends Camera {
	
	private final PCMResourceSetPartition set;
	
	public LessInvasiveInMemoryCamera(final LessInvasiveInMemoryRecord record, final SimulationEngine engine,
			final PCMResourceSetPartition set, final Collection<SPDAdjustorStateValues> policyIdToValues) {
		super(record, engine, policyIdToValues);
		this.set = set;
	}

	@Override
	public Snapshot takeSnapshot() {
		this.getScheduledReconfigurations().forEach(this::addEvent);

		final List<SPDAdjustorStateValues> values = this.snapStateValues();
		
		final Snapshot snapshot = new InMemorySnapshot(snapEvents(), values);
		return snapshot;
	}

	

	/**
	 * Collect and clone all state relevant events from the past and the future and adjust offsetts, if necessary.
	 *
	 * @return Set of events for recreating the state.
	 */
	private Set<DESEvent> snapEvents() {
		final Set<DESEvent> offsettedEvents = this.collectRelevantEvents();
		final Set<DESEvent> clonedEvents = (new CloneHelperWithVisitor(set)).clone(offsettedEvents);

		this.log(clonedEvents);
		
		clonedEvents.addAll(additionalEvents); // they are not cloned. maybe problematic? but we didn't clone them earlier either. 
		return clonedEvents;
	}
}
