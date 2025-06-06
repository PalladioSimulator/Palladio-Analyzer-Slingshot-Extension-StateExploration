package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
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
public final class JsonSnapshot implements Snapshot {

	private final String eventJson;

	private final List<ModelAdjustmentRequested> modelAdjustmentRequestedEvents;
	
	private final Collection<SPDAdjustorStateValues> adjustorStateValues;

	public JsonSnapshot(final String events, final Collection<SPDAdjustorStateValues> stateValues) {
		this.eventJson = events;
		this.modelAdjustmentRequestedEvents = new ArrayList<>();
		this.adjustorStateValues = stateValues;
	}


	@Override
	public Set<DESEvent> getEvents(final PCMResourceSetPartition set) {
		
		// Somehow deserialize the eventJson String. 
		
		return null; 
	}

	@Override
	public List<ModelAdjustmentRequested> getModelAdjustmentRequestedEvent() {
		return this.modelAdjustmentRequestedEvents;
	}

	@Override
	public Collection<SPDAdjustorStateValues> getSPDAdjustorStateValues() {
		return this.adjustorStateValues;
	}
}