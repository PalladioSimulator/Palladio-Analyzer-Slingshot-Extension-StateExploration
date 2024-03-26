package org.palladiosimulator.analyzer.slingshot.snapshot.api;

import java.util.Optional;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

/**
 * TODO
 *
 * @author stiesssh
 *
 */
public interface Snapshot {

	/**
	 * Event to initialize the next simulation run on.
	 *
	 * @return
	 */
	public Set<DESEvent> getEvents();

	/**
	 * Adjustor of the reactive reconfiguration, if a reactive reconfiguration happened to end the state.
	 *
	 * @return
	 */
	public Optional<ModelAdjustmentRequested> getModelAdjustmentRequestedEvent();

	public void setModelAdjustmentRequestedEvent(ModelAdjustmentRequested event);
}