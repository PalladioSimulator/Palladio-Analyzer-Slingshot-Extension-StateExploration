package org.palladiosimulator.analyzer.slingshot.planner.data;

public record ReconfigurationChange(String scalingPolicyId, double timeStamp) implements Change {

}
