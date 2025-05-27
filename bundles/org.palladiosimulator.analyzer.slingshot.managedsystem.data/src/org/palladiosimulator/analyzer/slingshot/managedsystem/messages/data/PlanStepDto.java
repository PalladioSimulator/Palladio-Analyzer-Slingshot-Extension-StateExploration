package org.palladiosimulator.analyzer.slingshot.managedsystem.messages.data;

import java.util.List;
import java.util.UUID;

import org.palladiosimulator.spd.ScalingPolicy;

public record PlanStepDto(Double time, List<ScalingPolicy> policies, UUID id, UUID stateId) {

}