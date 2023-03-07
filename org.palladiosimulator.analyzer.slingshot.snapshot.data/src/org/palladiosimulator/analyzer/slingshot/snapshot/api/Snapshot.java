package org.palladiosimulator.analyzer.slingshot.snapshot.api;

import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.simulation.events.DESEvent;

/**
 * TODO
 *
 * @author stiesssh
 *
 */
public interface Snapshot {

	//public String getId();

	public Set<DESEvent> getEvents();

	//public void setEvents(final Set<DESEvent> events);

	//public Allocation getAllocation();

	//public void setAllocation(final Allocation allocation);

	//public MonitorRepository getMonitorRepository();

	//public void setMonitorRepository(final MonitorRepository monitorRepository);

}
