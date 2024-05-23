package org.palladiosimulator.analyzer.slingshot.stateexploration.messages;

import org.palladiosimulator.analyzer.slingshot.networking.data.SimulationEventMessage;

public class SimTestMessage extends SimulationEventMessage<String> {

	public static final String MESSAGE_MAPPING_IDENTIFIER = "TestSimEvent";

	public SimTestMessage(final String payload) {
		super(MESSAGE_MAPPING_IDENTIFIER, payload);
	}

}
