package org.palladiosimulator.analyzer.slingshot.managedSystem.injection.messages;

import org.palladiosimulator.analyzer.slingshot.managedSystem.injection.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 * Announces that a new state was explored.
 *
 * @author Raphael Straub, Sarah Stieß
 *
 */
public class StateExploredEventMessage extends EventMessage<StateGraphNode> {

	public static final String MESSAGE_MAPPING_IDENTIFIER = "StateExplored";

	public StateExploredEventMessage(final StateGraphNode payload) {
		super(MESSAGE_MAPPING_IDENTIFIER, payload);
	}
}
