package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

public class RequestArchitectureMessage extends EventMessage<String> {

	public RequestArchitectureMessage(final String payload) {
		super("RequestArchitecture", payload);
	}
}
