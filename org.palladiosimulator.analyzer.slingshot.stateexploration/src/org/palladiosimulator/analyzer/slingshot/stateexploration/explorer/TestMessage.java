package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 * Proof of Concept
 */
public class TestMessage extends EventMessage<String> {

	public TestMessage(final String payload) {
		super("Test", payload);
	}
}
