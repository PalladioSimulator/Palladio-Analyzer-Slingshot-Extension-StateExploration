package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.providers;

import javax.inject.Provider;
import javax.inject.Singleton;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

/**
 * A provider for the {@link SimuComConfig} object that holds
 * all the information about the simulation.
 *
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
