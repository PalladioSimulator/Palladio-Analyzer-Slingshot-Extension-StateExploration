package org.palladiosimulator.analyzer.slingshot.stateexploration.providers;

import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

/**
 * Wrapper for a the set of {@link DESEvent} to initialise the next simulation
 * run.
 *
 * We use this wrapper, because creating an provider, that directly provides a
 * Set of something did not work. Probably some Problem with the Types, but i am
 * no expert [Stieß]
 *
 * @author Sarah Stieß
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
