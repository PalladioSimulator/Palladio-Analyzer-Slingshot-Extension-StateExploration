package org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.data;

import java.util.List;

import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * @author Sophie Stieß
 *
 */
public class OtherInitThings {
	
	private final boolean isRootSuccesor;
	private final double sensibility; 
	private final List<ScalingPolicy> incomingPolicies;

	public OtherInitThings(final boolean isRootSuccesor, final double sensibility, final List<ScalingPolicy> incomingPolicies) {
		super();
		this.isRootSuccesor = isRootSuccesor;
		this.sensibility = sensibility;
		this.incomingPolicies = incomingPolicies;
	}

	public boolean isRootSuccesor() {
		return isRootSuccesor;
	}

	public double getSensibility() {
		return sensibility;
	}

	public List<ScalingPolicy> getIncomingPolicies() {
		return incomingPolicies;
	}
}
