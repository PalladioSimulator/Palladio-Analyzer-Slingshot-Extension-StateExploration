package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

import java.util.List;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;

import com.google.common.base.Preconditions;

/**
 *
 *
 */
public class ReactiveReconfiguration extends Reconfiguration {

	public ReactiveReconfiguration(final ModelAdjustmentRequested event) {
		super(List.of(Preconditions.checkNotNull(event)));
	}

	public ReactiveReconfiguration(final List<ModelAdjustmentRequested> events) {
		super(events);
	}
}
