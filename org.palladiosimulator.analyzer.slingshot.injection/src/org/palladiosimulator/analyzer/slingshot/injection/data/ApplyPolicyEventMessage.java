package org.palladiosimulator.analyzer.slingshot.injection.data;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 * @author Sarah Stieß
 *
 */
public class ApplyPolicyEventMessage extends EventMessage<Plan> {

	public static final String MESSAGE_MAPPING_IDENTIFIER = ApplyPolicyEventMessage.class.getSimpleName();

    public ApplyPolicyEventMessage(final String event, final Plan payload, final String creator) {
        super(event, payload, creator);
	}


	// Add Timing, i.e. *when* shall the policy be applied?
}

