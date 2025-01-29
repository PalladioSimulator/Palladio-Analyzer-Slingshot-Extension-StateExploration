package org.palladiosimulator.analyzer.slingshot.managedsystem.messages;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 * Announce that Managed System is finished.
 *
 * @author Sophie Stieß
 *
 */
public class ManagedSystemFinishedMessage extends EventMessage<String> {

    public static final String MESSAGE_MAPPING_IDENTIFIER = ManagedSystemFinishedMessage.class.getSimpleName();

    public ManagedSystemFinishedMessage(final String creator) {
        super(MESSAGE_MAPPING_IDENTIFIER, "Managed System simulation finisched", creator);
	}

}