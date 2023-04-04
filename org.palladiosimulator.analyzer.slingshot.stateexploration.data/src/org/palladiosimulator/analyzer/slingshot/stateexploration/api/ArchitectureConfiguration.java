package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.spd.SPD;

/**
 * Alloc + Monitoring + SPD + SLO, just all the models that change.
 *
 * And at this point i'm not event sure whether it still makes sense to have
 * ArchitectureConfiguration or whether i just switch to PCM partitions, or sth.
 * like that.
 *
 */
public interface ArchitectureConfiguration {

	public MonitorRepository getMonitorRepository();

	public Allocation getAllocation();

	public SPD getSPD();

	public ServiceLevelObjectiveRepository getSLOs();

	public ArchitectureConfiguration copy();
}
