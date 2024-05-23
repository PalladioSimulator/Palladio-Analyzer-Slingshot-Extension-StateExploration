package org.palladiosimulator.analyzer.slingshot.snapshot.api;

import java.util.Optional;
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
	 * @return {@link ModelAdjustmentRequested} event, that ended the snapshotted
	 *         state, or empty if the state did not end with a reconfiguration.
	 */
	public Optional<ModelAdjustmentRequested> getModelAdjustmentRequestedEvent();

	/**
	 *
	 * @param event {@link ModelAdjustmentRequested} event, that ended the
	 *              snapshotted state.
	 */
	public void setModelAdjustmentRequestedEvent(ModelAdjustmentRequested event);
}