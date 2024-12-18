package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.spd.ScalingPolicy;

public abstract class Reconfiguration implements Change {

	private final ModelAdjustmentRequested event;

	public Reconfiguration(final ModelAdjustmentRequested event) {
		super();
		this.event = event;
	}

	public ModelAdjustmentRequested getReactiveReconfigurationEvent() {
		return event;
	}

	public ScalingPolicy getAppliedPolicy() {
		return event.getScalingPolicy();
	}
}
