package org.palladiosimulator.analyzer.slingshot.snapshot.api;

import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

/**
 *
 * Snaphshot of a Simulation run.
 *
 * @author Sarah Stieß
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
	 * @return {@link ModelAdjustmentRequested} event, that are scheduled for the
	 *         point in time the snapshot is taken state, or empty if no
	 *         reconfigurations happen at the end of the state.
	 */
	public Set<ModelAdjustmentRequested> getModelAdjustmentRequestedEvent();

	/**
	 *
	 * @param event {@link ModelAdjustmentRequested} event, that happens at the end
	 *              of the snapshotted state.
	 */
	public void addModelAdjustmentRequestedEvent(ModelAdjustmentRequested event);
}