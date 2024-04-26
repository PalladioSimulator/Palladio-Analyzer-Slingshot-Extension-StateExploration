package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.networking.events.SimulationEventMessage;

public class SimTestMessage extends SimulationEventMessage<String> {

	public SimTestMessage(String payload) {
		super("TestSimEvent", payload);
	}

}
