package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.networking.data.SimulationEventMessage;

public class SimTestMessage extends SimulationEventMessage<String> {

	public SimTestMessage(final String payload) {
		super("TestSimEvent", payload);
	}

}
