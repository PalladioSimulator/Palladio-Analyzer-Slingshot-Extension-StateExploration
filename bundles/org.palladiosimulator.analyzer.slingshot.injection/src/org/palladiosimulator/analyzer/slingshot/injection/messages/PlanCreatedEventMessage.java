package org.palladiosimulator.analyzer.slingshot.injection.messages;

import java.util.List;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 * @author Sarah Stieß
 *
 */
public class PlanCreatedEventMessage extends EventMessage<List<String>> {

    public static final String MESSAGE_MAPPING_IDENTIFIER = "PlanCreated";

    public PlanCreatedEventMessage(final String event, final List<String> payload, final String creator) {
        super(event, payload, creator);
	}

}

