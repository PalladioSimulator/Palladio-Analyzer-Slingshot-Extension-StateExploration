package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.providers;

import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

/**
 *
 */
public class EventsToInitOnWrapper {

	private final Set<DESEvent> eventsToInitOn;

	public EventsToInitOnWrapper(final Set<DESEvent> eventsToInitOn) {
		this.eventsToInitOn = eventsToInitOn;
	}

	public Set<DESEvent> getEventsToInitOn() {
		return eventsToInitOn;
	}
}
