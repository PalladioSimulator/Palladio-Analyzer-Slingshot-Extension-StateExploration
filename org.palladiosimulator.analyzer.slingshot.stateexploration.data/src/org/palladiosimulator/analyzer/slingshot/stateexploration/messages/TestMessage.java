package org.palladiosimulator.analyzer.slingshot.stateexploration.messages;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 * Proof of Concept
 */
public class TestMessage extends EventMessage<String> {

	public static final String MESSAGE_MAPPING_IDENTIFIER = "Test";

	public TestMessage(final String payload) {
		super(MESSAGE_MAPPING_IDENTIFIER, payload);
	}
}
