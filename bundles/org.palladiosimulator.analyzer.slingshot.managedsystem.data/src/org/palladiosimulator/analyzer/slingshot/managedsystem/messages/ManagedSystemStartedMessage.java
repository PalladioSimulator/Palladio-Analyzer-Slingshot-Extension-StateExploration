package org.palladiosimulator.analyzer.slingshot.managedsystem.messages;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 * Announce that Managed System is finished.
 *
 * @author Sophie Stieß
 *
 */
public class ManagedSystemStartedMessage extends EventMessage<String> {

    public static final String MESSAGE_MAPPING_IDENTIFIER = ManagedSystemStartedMessage.class.getSimpleName();

    public ManagedSystemStartedMessage(final String creator) {
        super(MESSAGE_MAPPING_IDENTIFIER, "Managed System simulation stared", creator);
	}

}