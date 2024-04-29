package org.palladiosimulator.analyzer.slingshot.stateexploration.providers;

import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Provides the event to init the next simulation run on.
 */
@Singleton
public class EventsToInitOnProvider implements Provider<EventsToInitOnWrapper> {

	private EventsToInitOnWrapper eventsToInitOn;

	public void set(final EventsToInitOnWrapper eventsToInitOn) {
		this.eventsToInitOn = eventsToInitOn;
	}

	@Override
	public EventsToInitOnWrapper get() {
		return eventsToInitOn;
	}

}
