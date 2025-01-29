package org.palladiosimulator.analyzer.slingshot.managedsystem.messages;

import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.data.AdaptationPlanDto;
import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 * @author Sarah Stieß
 *
 *
 */
public class PlanCreatedEventMessage extends EventMessage<AdaptationPlanDto> {

    public static final String MESSAGE_MAPPING_IDENTIFIER = "PlanCreated";

    public PlanCreatedEventMessage(final String event, final AdaptationPlanDto payload, final String creator) {
        super(event, payload, creator);
	}

}
