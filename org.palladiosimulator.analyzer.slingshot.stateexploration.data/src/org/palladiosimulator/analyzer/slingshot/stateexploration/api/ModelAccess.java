package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import java.util.Optional;

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
import org.scaledl.usageevolution.UsageEvolution;

/**
 * Provide access to PCM models.
 *
 * Mandatory models are always available. Optional models may or may not be
 * available, thus their respective getters either return an Optional containing
 * the model instance, or an empty optional.
 *
 */
public interface ModelAccess {

	public Repository getRepository();
	public System getSystem();
	public ResourceEnvironment getResourceEnvironment();
	public Allocation getAllocation();
	public UsageModel getUsageModel();


	public Optional<MonitorRepository> getMonitorRepository();
	public Optional<MeasuringPointRepository> getMeasuringPointRepository();
	public Optional<ServiceLevelObjectiveRepository> getSLOs();
	public Optional<SPD> getSPD();
	public Optional<Configuration> getSemanticSPDConfiguration();
	public Optional<UsageEvolution> getUsageEvolution();
}
