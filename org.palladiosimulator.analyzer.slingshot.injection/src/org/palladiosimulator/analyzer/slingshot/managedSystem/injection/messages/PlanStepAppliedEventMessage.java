package org.palladiosimulator.analyzer.slingshot.managedSystem.injection.messages;

import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.managedSystem.injection.messages.PlanStepAppliedEventMessage.PlanStep;
import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 * EventMessage to indicate the application of a step in the plan to the managed system.
 *
 * @author Sarah Stieß
 *
 */
public class PlanStepAppliedEventMessage extends EventMessage<PlanStep> {

    public static final String MESSAGE_MAPPING_IDENTIFIER = "PlanStepApplied";

    public PlanStepAppliedEventMessage(final PlanStep payload, final String creator) {
        super(MESSAGE_MAPPING_IDENTIFIER, payload, creator);
	}

    public record PlanStep(Double pointInTime, Set<String> policies) {

    }
}

