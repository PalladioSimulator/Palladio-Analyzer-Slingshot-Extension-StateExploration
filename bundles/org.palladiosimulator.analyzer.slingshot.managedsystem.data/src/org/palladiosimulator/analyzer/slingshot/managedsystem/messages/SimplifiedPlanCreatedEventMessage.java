package org.palladiosimulator.analyzer.slingshot.managedsystem.messages;

import java.util.List;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 * A simplified message to signify the creation of a plan.
 *
 * Holds a list of strings as payload. Each string is the id of a scaling policy from the SPD model of the curren simulation.
 *
 * The scaling policies with the given ids are applied immediately in the given order.
 *
 * For the reinforcement learning comparison run.
 *
 * @author Sophie Stieß
 *
 *
 */
public class SimplifiedPlanCreatedEventMessage extends EventMessage<List<String>> {

    public static final String MESSAGE_MAPPING_IDENTIFIER = "SimplifiedPlanCreated";

    public SimplifiedPlanCreatedEventMessage(final String event, final List<String> payload, final String creator) {
        super(event, payload, creator);
	}

}
