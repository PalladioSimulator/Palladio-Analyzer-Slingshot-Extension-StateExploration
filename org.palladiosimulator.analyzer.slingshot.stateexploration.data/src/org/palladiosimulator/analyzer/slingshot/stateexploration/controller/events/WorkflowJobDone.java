package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSystemEvent;

/**
 * Event to abbounce the end of the job to the
 * {@code ExplorerControllerSystemBehaviour}.
 *
 * @author Sarah Stieß
 *
 */
public class WorkflowJobDone extends AbstractSystemEvent implements ExplorationControllerEvent {

	public WorkflowJobDone() {
	}

}
