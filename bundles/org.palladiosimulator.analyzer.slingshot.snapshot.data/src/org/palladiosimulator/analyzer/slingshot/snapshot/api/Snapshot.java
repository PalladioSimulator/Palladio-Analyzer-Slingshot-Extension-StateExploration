package org.palladiosimulator.analyzer.slingshot.snapshot.api;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

/**
 *
 * Snapshot of a Simulation run.
 *
 * @author Sophie Stieß
 *
 */
public interface Snapshot {

	/**
	 *
	 * Get the events initialise the next simulation run on.
	 *
	 * All returned events are copies, as a simulation run must not affect the
	 * snapshot it was initialised on. The slingshot entities in the copied events
	 * reference PCM instances from the given {@link PCMResourceSetPartition}.
	 *
	 * @param set contains the PCM instances to be referenced in the next simulation
	 *            run.
	 * @return events to initialise the next simulation run on.
	 */
	public Set<DESEvent> getEvents(final PCMResourceSetPartition set);

	/**
	 *
	 * @return {@link ModelAdjustmentRequested} events, that happened at the end of
	 *         the state. state, or empty if the state did not end with a
	 *         reconfiguration.
	 */
	public List<ModelAdjustmentRequested> getModelAdjustmentRequestedEvent();
	
	/**
	 *
	 * @param event {@link ModelAdjustmentRequested} event, that ended the
	 *              snapshotted state.
	 */
	public void addModelAdjustmentRequestedEvent(ModelAdjustmentRequested event);
	
	/**
	 * Get the state of the {@code SPDAdjustorState} at the end of this state.
	 * 
	 * Needed start a new simulation run that resumes the simulation at the end of this state.
	 * 
	 * @return state of the {@code SPDAdjustorState} at the end of this state.
	 */
	public Collection<SPDAdjustorStateValues> getSPDAdjustorStateValues();

	/**
	 * 
	 * 
	 * @param value value to be added to this snapshot
	 */
	public void addSPDAdjustorStateValues(final SPDAdjustorStateValues value);
}