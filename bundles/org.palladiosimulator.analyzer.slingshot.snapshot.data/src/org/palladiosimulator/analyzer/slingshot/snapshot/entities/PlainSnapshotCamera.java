package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Camera;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;

/**
 * 
 * Camera to create a plain Snapshot. 
 * 
 * 
 * @author Sophie Stieß
 *
 */
public final class PlainSnapshotCamera extends Camera {
	
	public PlainSnapshotCamera(final LessInvasiveInMemoryRecord record, final SimulationEngine engine, final Collection<SPDAdjustorStateValues> policyIdToValues) {
		super(record, engine, policyIdToValues);
	}

	@Override
	public Snapshot takeSnapshot() {
		this.getScheduledReconfigurations().forEach(this::addEvent);
		final Collection<SPDAdjustorStateValues> values = this.snapStateValues();
		
		final Set<DESEvent> todoEvents = new HashSet<>(this.collectAndOffsetEvents());
		todoEvents.addAll(additionalEvents); 
			
		return new PlainSnapshot(todoEvents, values);
	}
}
