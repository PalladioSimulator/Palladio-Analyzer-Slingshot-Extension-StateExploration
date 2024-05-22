package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;

/**
 *
 * Event to trigger the {@link GraphExplorer} to explore only one state.
 *
 * @author Sarah Stieß
 *
 */
public class IdleTriggerExplorationEvent extends TriggerExplorationEvent {
	public IdleTriggerExplorationEvent() {
		super(1);
	}
}
