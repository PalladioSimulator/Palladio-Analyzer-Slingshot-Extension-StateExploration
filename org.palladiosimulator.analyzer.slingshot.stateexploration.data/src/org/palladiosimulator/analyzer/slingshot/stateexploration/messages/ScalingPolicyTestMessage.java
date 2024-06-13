package org.palladiosimulator.analyzer.slingshot.stateexploration.messages;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 * Proof of Concept
 */
public class ScalingPolicyTestMessage extends EventMessage<ScalingPolicy> {

	public ScalingPolicyTestMessage(final ScalingPolicy payload) {
		super("Test", payload);
	}
}
