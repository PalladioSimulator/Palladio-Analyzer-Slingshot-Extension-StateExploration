package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;

/**
 *
 *
 */
public class ReactiveReconfiguration extends Reconfiguration {

	public ReactiveReconfiguration(final ModelAdjustmentRequested event) {
		super(event);
	}

}
