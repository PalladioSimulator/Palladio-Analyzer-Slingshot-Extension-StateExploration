package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSystemEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;

/**
 *
 * Event to trigger the {@link GraphExplorer} to explore only one state.
 *
 * @author Sarah Stieß
 *
 */
public class IdleTriggerExplorationEvent extends AbstractSystemEvent implements ExplorationControllerEvent {
	public IdleTriggerExplorationEvent() {
		super();
	}
}
