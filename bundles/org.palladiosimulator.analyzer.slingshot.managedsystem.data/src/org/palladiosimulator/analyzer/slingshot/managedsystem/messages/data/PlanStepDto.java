package org.palladiosimulator.analyzer.slingshot.managedsystem.messages.data;

import java.util.List;
import java.util.UUID;

public record PlanStepDto(Double time, List<UUID> policies, UUID id, UUID stateId) {

}