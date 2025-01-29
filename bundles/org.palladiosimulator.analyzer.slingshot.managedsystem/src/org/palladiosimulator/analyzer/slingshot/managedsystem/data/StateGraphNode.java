package org.palladiosimulator.analyzer.slingshot.managedsystem.data;

import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * Slimmed down version of a node in the state graph, with only the information required for
 * injecting policies.
 *
 * Beware: Naming of record components must match the field names in the JSON or else the
 * deserialization does not work. there is probably a work around for this, but i do not know it.
 *
 * @author Sarah Stieß
 *
 */
public record StateGraphNode(String id, double startTime, ScalingPolicy incomingPolicy) {

}
