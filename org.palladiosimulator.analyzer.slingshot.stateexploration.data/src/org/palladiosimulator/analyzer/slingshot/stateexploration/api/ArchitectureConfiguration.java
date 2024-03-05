package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.semanticspd.Configuration;
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

	public ResourceEnvironment getResourceEnvironment();

	public System getSystem();

	public SPD getSPD();
	
	public Configuration getSemanticSPDConfiguration();

	public ServiceLevelObjectiveRepository getSLOs();

	public MeasuringPointRepository getMeasuringPointRepository();

	public UsageModel getUsageModel();

	public ArchitectureConfiguration copy();

	public void transferModelsToSet(final ResourceSet set);
}
