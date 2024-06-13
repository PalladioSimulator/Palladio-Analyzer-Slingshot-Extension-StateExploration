package org.palladiosimulator.analyzer.slingshot.injection.messages;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 * @author Sarah Stieß
 *
 */
public class ManagedSystemTimeEventMessage extends EventMessage<Double> {

    public static final String MESSAGE_MAPPING_IDENTIFIER = ManagedSystemTimeEventMessage.class.getSimpleName();

    public ManagedSystemTimeEventMessage(final Double payload, final String creator) {
        super(MESSAGE_MAPPING_IDENTIFIER, payload, creator);
	}

}

