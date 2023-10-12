package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 *
 */
public class ReactiveReconfiguration extends Reconfiguration {


	private final DESEvent event;

	public ReactiveReconfiguration(final ScalingPolicy policy, final DESEvent event) {
		super(policy);
		this.event = event;
	}

	public DESEvent getReactiveReconfigurationEvent() {
		return event;
	}

	@Override
	public String toString() {
		return String.format("%s-reconfiguration", event.getName());
	}
}
