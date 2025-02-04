package org.palladiosimulator.analyzer.slingshot.stateexploration.providers;

import java.util.List;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateInitialized;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

/**
 * Wrapper for a the set of {@link DESEvent} to initialise the next simulation
 * run.
 *
 * We use this wrapper, because creating an provider, that directly provides a
 * Set of something did not work. Probably some Problem with the Types, but i am
 * no expert. [S3]
 *
 * @author Sophie Stieß
 *
 */
public class EventsToInitOnWrapper {

	/**
	 * Adjustments to be applied at the beginning of the simulation run. Beware, the order is relevant. 
	 */
	private final List<ModelAdjustmentRequested> adjustmentEvents;
	
	private final Set<SPDAdjustorStateInitialized> stateInitEvents;
	private final Set<DESEvent> otherEvents;

	/**
	 * Create a wrapper around the event to initialise a simulation run. 
	 * 
	 * @param adjustmentEvents Adjustments to be applied at the beginning of the simulation run.
	 * @param stateInitEvents Values to initialise the states of the SPD interpreter.
	 * @param otherEvents User and request events for the simulation run.
	 */
	public EventsToInitOnWrapper(final List<ModelAdjustmentRequested> adjustmentEvents, final Set<SPDAdjustorStateInitialized> stateInitEvents, Set<DESEvent> otherEvents) {
		
		this.adjustmentEvents = adjustmentEvents;
		this.otherEvents = otherEvents;
		this.stateInitEvents = stateInitEvents;
	}

	public List<ModelAdjustmentRequested> getAdjustmentEvents() {
		return adjustmentEvents;
	}

	public Set<SPDAdjustorStateInitialized> getStateInitEvents() {
		return stateInitEvents;
	}

	public Set<DESEvent> getOtherEvents() {
		return otherEvents;
	}


}
