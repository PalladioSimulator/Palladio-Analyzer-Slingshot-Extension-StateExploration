package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.networking.ws.EventMessage;

/**
 * Proof of Concept
 */
public class TestMessage extends EventMessage<String> {

	public TestMessage(String payload) {
		super("Test", payload);
	}
}