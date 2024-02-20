package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.networking.ws.SimulationEventMessage;

public class SimTestMessage extends SimulationEventMessage<String> {

	public SimTestMessage(String payload) {
		super("TestSimEvent", payload);
	}

}
