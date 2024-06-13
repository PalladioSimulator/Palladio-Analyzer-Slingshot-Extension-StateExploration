package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 * TODO
 *
 * @author Sarah Stieß
 *
 */
public class ResetExplorerEvent extends EventMessage<String> implements ExplorationControllerEvent {

	public static final String MESSAGE_MAPPING_IDENTIFIER = "ResetSystem";

	public ResetExplorerEvent() {
		super(MESSAGE_MAPPING_IDENTIFIER, null);
	}

}
