package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;

/**
 *
 *
 */
public class ReactiveReconfiguration implements Change {


	private final ModelAdjustmentRequested event;

	public ReactiveReconfiguration(final ModelAdjustmentRequested event) {
		super();
		this.event = event;
	}

	public ModelAdjustmentRequested getReactiveReconfigurationEvent() {
		return event;
	}

	@Override
	public String toString() {
		return String.format("%s-REactive reconfiguration", event.getName());
	}
}
