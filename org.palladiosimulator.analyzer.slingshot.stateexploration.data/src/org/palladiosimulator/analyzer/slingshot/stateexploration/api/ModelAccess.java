package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.spd.SPD;

/**
 * 
 *
 */
public interface ModelAccess {
	// Getter for PCM models
	public Repository getRepository();

	public System getSystem();

	public ResourceEnvironment getResourceEnvironment();

	public Allocation getAllocation();

	public UsageModel getUsageModel();

	public MonitorRepository getMonitorRepository();

	public MeasuringPointRepository getMeasuringPointRepository();

	public ServiceLevelObjectiveRepository getSLOs();

	// Getter for SPD models
	public SPD getSPD();

	public Configuration getSemanticSPDConfiguration();
}
