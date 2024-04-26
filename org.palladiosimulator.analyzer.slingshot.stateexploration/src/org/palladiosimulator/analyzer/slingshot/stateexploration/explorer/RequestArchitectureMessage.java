package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.networking.events.EventMessage;

public class RequestArchitectureMessage extends EventMessage<String> {

	public RequestArchitectureMessage(String payload) {
		super("RequestArchitecture", payload);
	}
}
