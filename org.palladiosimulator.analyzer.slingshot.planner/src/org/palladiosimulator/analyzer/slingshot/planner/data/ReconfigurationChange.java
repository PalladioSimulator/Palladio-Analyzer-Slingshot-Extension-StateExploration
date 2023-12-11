package org.palladiosimulator.analyzer.slingshot.planner.data;

import org.palladiosimulator.spd.ScalingPolicy;

public record ReconfigurationChange(ScalingPolicy scalingPolicy, double timeStamp) implements Change {

}
