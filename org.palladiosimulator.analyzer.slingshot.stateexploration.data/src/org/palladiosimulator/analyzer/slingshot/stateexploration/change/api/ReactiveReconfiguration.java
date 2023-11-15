package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

/**
 *
 *
 */
public class ReactiveReconfiguration implements Change {


	private final DESEvent event;

	public ReactiveReconfiguration(final DESEvent event) {
		super();
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
