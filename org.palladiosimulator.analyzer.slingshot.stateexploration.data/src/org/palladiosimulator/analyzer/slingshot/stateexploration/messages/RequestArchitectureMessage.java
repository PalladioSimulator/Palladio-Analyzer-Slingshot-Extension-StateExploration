package org.palladiosimulator.analyzer.slingshot.stateexploration.messages;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 * @author Raphael Straub
 *
 */
public class RequestArchitectureMessage extends EventMessage<String> {

	public static final String MESSAGE_MAPPING_IDENTIFIER = "RequestArchitecture";

	public RequestArchitectureMessage(final String payload) {
		super(MESSAGE_MAPPING_IDENTIFIER, payload);
	}
}
