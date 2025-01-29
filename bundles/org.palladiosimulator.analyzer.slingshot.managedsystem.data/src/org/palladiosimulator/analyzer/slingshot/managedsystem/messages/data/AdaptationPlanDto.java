package org.palladiosimulator.analyzer.slingshot.managedsystem.messages.data;

import java.util.List;
import java.util.UUID;

public record AdaptationPlanDto(UUID id, UUID explorationId, List<PlanStepDto> plan, UUID currentPlanStep,
        double maxUtility) {

}