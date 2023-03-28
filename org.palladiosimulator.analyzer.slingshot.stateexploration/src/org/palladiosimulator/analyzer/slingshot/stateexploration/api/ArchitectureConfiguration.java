package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.spd.SPD;

/**
 * Alloc + Monitoring, very plain, nothing else.
 *
 */
public interface ArchitectureConfiguration {

	public MonitorRepository getMonitorRepository();
	public Allocation getAllocation();
	public SPD getSPD();

	public ArchitectureConfiguration copy();
}
