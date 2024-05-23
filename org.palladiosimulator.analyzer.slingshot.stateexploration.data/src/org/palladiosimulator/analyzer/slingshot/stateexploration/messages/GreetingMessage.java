package org.palladiosimulator.analyzer.slingshot.stateexploration.messages;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 * Proof of Concept
 */
public class GreetingMessage extends EventMessage<String> {

	public static final String MESSAGE_MAPPING_IDENTIFIER = "Greetings";

	public GreetingMessage(final String payload, final String creator) {
		super(MESSAGE_MAPPING_IDENTIFIER, payload, creator);
	}
}
