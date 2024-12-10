package org.palladiosimulator.analyzer.slingshot.injection.messages;

import java.util.Set;
import java.util.UUID;

import org.palladiosimulator.analyzer.slingshot.injection.messages.PlanStepAppliedEventMessage.PlanStep;
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

    private PlanStepAppliedEventMessage(final PlanStep payload, final String creator) {
        super(MESSAGE_MAPPING_IDENTIFIER, payload, creator);
    }

    public record PlanStep(Double pointInTime, Set<String> policies) {

    }

    /**
     *
     * Factory operation, to ensure, that the oddly static {@link EventMessage#EXPLORATION_ID} is
     * set before calling the constructor.
     *
     * @param payload
     * @param explorationId
     * @param creator
     * @return
     */
    public static PlanStepAppliedEventMessage of(final PlanStep payload, final UUID explorationId,
            final String creator) {
        EventMessage.EXPLORATION_ID = explorationId;
        return new PlanStepAppliedEventMessage(payload, creator);
    }
}
