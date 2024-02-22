package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated; // TODO DELETE, for DEUBG only!!
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationDriver;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.planner.runner.StateGraphConverter;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemorySnapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration.SimulationInitConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration.UriBasedArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.DefaultExplorationPlanner;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.workflow.WorkflowConfigurationModule;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentPackage;
import org.palladiosimulator.pcm.system.SystemPackage;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.spd.SPD;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * Core Component of the Exploration.
 *
 * Responsible for starting the simulation runs to explore different branches.
 *
 * @author stiesssh
 *
 */
public class DefaultGraphExplorer implements GraphExplorer {

	private static final Logger LOGGER = Logger.getLogger(DefaultGraphExplorer.class.getName());

	/** content changes with each iteration */
	private final PCMResourceSetPartition initModels;

	private final Map<String, Object> launchConfigurationParams;

	private final DefaultExplorationPlanner blackbox;

	private final DefaultGraph graph;

	private final IProgressMonitor monitor;
	
	private final SystemDriver systemDriver = Slingshot.getInstance().getSystemDriver();

	private final SimpleDirectedGraph jGraphGraph;

	private final MDSDBlackboard blackboard;

	public DefaultGraphExplorer(final PCMResourceSetPartition partition, final SimulationDriver driver,
			final Map<String, Object> launchConfigurationParams, final IProgressMonitor monitor,
			final MDSDBlackboard blackboard) {
		super();
		this.initModels = partition;
		this.launchConfigurationParams = launchConfigurationParams;
		this.monitor = monitor;
		this.blackboard = blackboard;

		EcoreUtil.resolveAll(initModels.getResourceSet());

		final MonitorRepository monitorRepository = PCMResourcePartitionHelper.getMonitorRepository(partition);
		final MeasuringPointRepository measuringPointRepository = PCMResourcePartitionHelper
				.getMeasuringPointRepository(partition);
		final SPD spd = PCMResourcePartitionHelper.getSPD(partition);
		final Configuration semanticSpd = PCMResourcePartitionHelper.getSemanticSPD(partition);
		final ServiceLevelObjectiveRepository sloRepository = PCMResourcePartitionHelper.getSLORepository(partition);

		this.graph = new DefaultGraph(this.createRoot(this.initModels.getAllocation(), this.initModels.getUsageModel(),
				monitorRepository, measuringPointRepository, spd, semanticSpd, sloRepository));

		this.blackbox = new DefaultExplorationPlanner(spd, this.graph);

		this.jGraphGraph = new SimpleDirectedGraph<>(RawTransition.class);
	}

	@Override
	public RawStateGraph start() {
		LOGGER.info("********** DefaultGraphExplorer.start **********");

		for (int i = 0; i < 100; i++) { // just random.
			if (!this.graph.hasNext()) {
				LOGGER.info(String.format("Fringe is empty. Stop Exloration after %d iterations.", i));
				break;
			}
			final SimulationInitConfiguration config = this.blackbox.createConfigForNextSimualtionRun();

			this.exploreBranch(config);
		}
		LOGGER.warn("********** DefaultGraphExplorer is done :) **********");
		LOGGER.warn("********** States : ");
		this.graph.getStates()
				.forEach(s -> LOGGER.warn(String.format("%s : %.2f -> %.2f, duration : %.2f,  reason: %s ", s.getId(),
						s.getStartTime(), s.getEndTime(), s.getDuration(), s.getReasonToLeave())));
		LOGGER.warn("********** Transitions : ");
		this.graph.getTransitions().stream().forEach(
				t -> LOGGER.warn(String.format("%s : %.2f type : %s", t.getName(), t.getPointInTime(), t.getType())));
		return this.graph;
	}

	/**
	 * Create root node for the graph. Sadly, ExperimentSettings for root are null
	 * :/
	 */
	private DefaultState createRoot(final Allocation alloc, final UsageModel usageModel,
			final MonitorRepository monitoring, final MeasuringPointRepository measuringPoints, final SPD spd,
			final Configuration semanticpd, final ServiceLevelObjectiveRepository slo) {
		// final ArchitectureConfiguration rootConfig = new
		// DefaultArchitectureConfiguration(alloc, monitoring, spd);
		final ArchitectureConfiguration rootConfig = UriBasedArchitectureConfiguration.builder().withAllocation(alloc)
				.withUsageModel(usageModel).withMonitorRepository(monitoring).withSPD(spd).withSLO(slo)
				.withSemanticSpdConfigruation(semanticpd).withMeasuringPointRepository(measuringPoints).build();

		final Snapshot initSnapshot = new InMemorySnapshot(Set.of());

		final DefaultState root = new DefaultState(0.0, rootConfig);
		systemDriver.postEvent(new StateExploredMessage(StateGraphConverter.convertState(root, null)));
		root.setSnapshot(initSnapshot);
		return root;
	}

	/**
	 *
	 * Creates tons of configurations from the given configuration and uses them so
	 * start a simulation run.
	 *
	 * @param config
	 */
	private void exploreBranch(final SimulationInitConfiguration config) {
		// update provided models
		this.updatePCMPartitionProvider(config);
		// update simucomconfig
		final SimuComConfig simuComConfig = prepareSimuComConfig(config.getStateToExplore()
				.getArchitecureConfiguration().getAllocation().eResource().getURI().toString(),
				config.getExplorationDuration());
		// ????
		final SnapshotConfiguration snapConfig = createSnapConfig(config.getExplorationDuration(),
				!config.getSnapToInitOn().getEvents().isEmpty());

		// TODO *somehow* get this submodule into the driver, such that it will be
		// provided D:
		final SubModule submodule = new SubModule(config, snapConfig);

		WorkflowConfigurationModule.simuComConfigProvider.set(simuComConfig);
		// Provider blckboard here, or provide it somewhere else?
		WorkflowConfigurationModule.blackboardProvider.set(blackboard);

		final SimulationDriver driver = Slingshot.getInstance().getSimulationDriver();

		LOGGER.warn("Run on Models at: " + config.getStateToExplore().getArchitecureConfiguration().getAllocation()
				.eResource().getURI().toString());

		LOGGER.warn("Start with Request to these Resources: ");
		config.getSnapToInitOn().getEvents().stream().filter(e -> e instanceof JobInitiated).map(e -> (JobInitiated) e)
				.filter(e -> e.getEntity() instanceof ActiveJob).map(e -> ((ActiveJob) e.getEntity())
						.getAllocationContext().getResourceContainer_AllocationContext().getId())
				.forEach(id -> LOGGER.info(id));

		if (config.getEvent().isPresent()) {
			LOGGER.warn("Start with reactive Configuration.");
		}
		if (config.getPolicy().isPresent()) {
			LOGGER.warn("Start with proactive Configuration.");
		}
		if (config.getEvent().isEmpty() && config.getPolicy().isEmpty()) {
			LOGGER.warn("Start after intervall transition.");
		}

		driver.init(simuComConfig, monitor, Set.of(submodule));
		driver.start();

		// Post processing :
		final DefaultState current = submodule.builder();
		systemDriver.postEvent(new StateExploredMessage(StateGraphConverter.convertState(current, config.getParentId())));
		this.blackbox.updateGraphFringePostSimulation(current);
	}

	/**
	 *
	 * @author stiesssh
	 *
	 */
	private class SubModule extends AbstractModule {

		private final Snapshot snapToInitOn;
		private final DefaultState currentPartialState;
		private final SnapshotConfiguration snapshotConfiguration;

		private final Set<DESEvent> eventToInitOn;

		public SubModule(final SimulationInitConfiguration config, final SnapshotConfiguration snapshotConfiguration) {

			this.snapToInitOn = config.getSnapToInitOn();
			this.currentPartialState = config.getStateToExplore();
			this.snapshotConfiguration = snapshotConfiguration;

			this.eventToInitOn = snapToInitOn.getEvents();

			if (config.getEvent().isPresent()) {
				this.eventToInitOn.add(config.getEvent().get());
			}
		}

		// PCM instance and SimuComConfig already provided via other means.
		@Provides
		public Snapshot snapToInitOn() {
			return snapToInitOn;
		}

		@Provides
		public Set<DESEvent> EventToInitOn() {
			return eventToInitOn;
		}

		@Provides
		public DefaultState builder() {
			return currentPartialState;
		}

		@Provides
		public SnapshotConfiguration snapshotConfig() {
			return snapshotConfiguration;
		}

		@Override
		protected void configure() {
		}

	}

	/**
	 *
	 * Create a SimuComConfig for the next simulation run.
	 *
	 * Set the experiment run to new value for each exploration, because each
	 * experiment run yields a new {@link ExperimentGroup} and each group represents
	 * a common goal, which is quite befitting in our case. (TBH though, i just want
	 * each exploration listed separately but am too lazy to change the
	 * configuration every time) Update the variation id for each simulation run,
	 * because each variation id yields a new {@link ExperimentSetting} and each
	 * setting is one alternative, which is quite befitting in our case.
	 * 
	 * Ensures, that the max simulation time is larger than the interval duration,
	 * otherwise {@link SimulationFinished} gets scheduled before
	 * {@link SnapshotInitiated}.
	 *
	 * TODO set variation id to something meaningfull, e.g. archConfig x change
	 *
	 * TODO set experiment run to something meaning full
	 *
	 * @param variation
	 * @param duration  duration of the interval in seconds.
	 * @return
	 */
	private SimuComConfig prepareSimuComConfig(final String variation, final double duration) {
		// MapHelper.getValue(configuration, VARIATION_ID, String.class)
		launchConfigurationParams.put(SimuComConfig.SIMULATION_TIME, String.valueOf(((long) duration) + 1));
		launchConfigurationParams.put(SimuComConfig.VARIATION_ID, variation);
		launchConfigurationParams.put(SimuComConfig.EXPERIMENT_RUN, this.graph.toString());
		//launchConfigurationParams.put(SimuComConfig.SIMULATION_TIME, String.valueOf(((long) duration) + 1));


		return new SimuComConfig(launchConfigurationParams, true);
	}

	/**
	 *
	 * Create the SnapshotConfiguration required to start a new simulation run.
	 *
	 * @param interval between two snapshots
	 * @param init     wether or not to init on a snapshot.
	 * @return
	 */
	private SnapshotConfiguration createSnapConfig(final double interval, final boolean init) {
		return new SnapshotConfiguration(interval, init, 0.5);
	}

	/**
	 * Put the copied models into the {@link PCMResourceSetPartitionProvider}.
	 *
	 * I'm pretty sure the current implementation is pretty illegal, cause it
	 * updates alloc, monitor and spd only but disregards the other models.
	 *
	 * Does this implementatin fuck with proxies?
	 *
	 * Currently, injection only works because slingshot always injects allocation.
	 *
	 *
	 * @param allocation
	 * @param monitorRepository
	 */
	private void updatePCMPartitionProvider(final SimulationInitConfiguration config) {

		replaceResoucreContentForAllModels(config.getStateToExplore().getArchitecureConfiguration());

		/* add initial ScalingPolicy, if present */
		if (config.getPolicy().isPresent()) {
			PCMResourcePartitionHelper.getSPD(initModels).getScalingPolicies().add(config.getPolicy().get());
		}

		final PCMResourceSetPartitionProvider provider = Slingshot.getInstance()
				.getInstance(PCMResourceSetPartitionProvider.class);
		provider.set(this.initModels); // this is probably not needed.
	}

	/**
	 * 
	 * @param archConfig
	 */
	private void replaceResoucreContentForAllModels(ArchitectureConfiguration archConfig) {
		if (this.initModels.hasElement(SystemPackage.eINSTANCE.getSystem())) {
			// replaceResourceContent(archConfig.getSystem(), this.initModels.getSystem());
			replaceResourceContent(archConfig.getAllocation().getSystem_Allocation(), this.initModels.getSystem());
		} else {
			LOGGER.info("no System model, skip replacement");
		}
		if (this.initModels.hasElement(ResourceenvironmentPackage.eINSTANCE.getResourceEnvironment())) {
//			replaceResourceContent(archConfig.getResourceEnvironment(),
//				this.initModels.getResourceEnvironment());
			replaceResourceContent(archConfig.getAllocation().getTargetResourceEnvironment_Allocation(),
					this.initModels.getResourceEnvironment());

		} else {
			LOGGER.info("no ResourceEnvironment model, skip replacement");
		}
		if (this.initModels.hasElement(RepositoryPackage.eINSTANCE.getRepository())) {
			replaceRepositoryModel(archConfig);
		} else {
			LOGGER.info("no repository model, skip replacement");
		}

		replaceResourceContent(archConfig.getAllocation(), this.initModels.getAllocation());

		replaceResourceContent(archConfig.getMonitorRepository(),
				PCMResourcePartitionHelper.getMonitorRepository(initModels));
		replaceResourceContent(archConfig.getSPD(), PCMResourcePartitionHelper.getSPD(initModels));
		replaceResourceContent(archConfig.getSemanticSPDConfiguration(),
				PCMResourcePartitionHelper.getSemanticSPD(initModels));

		replaceResourceContent(archConfig.getSLOs(), PCMResourcePartitionHelper.getSLORepository(initModels));
		replaceResourceContent(archConfig.getMeasuringPointRepository(),
				PCMResourcePartitionHelper.getMeasuringPointRepository(initModels));

		replaceResourceContent(archConfig.getUsageModel(), this.initModels.getUsageModel());

	}

	/**
	 * 
	 * Replace a model.
	 * 
	 * @param <T>      type of the model to be replaced in the resource, bounded by
	 *                 {@code EObject} in stead of {@code Entity} because of
	 *                 {@link Configuration} is not an {@code Entity}.
	 * @param model    the new model
	 * @param oldModel the model to be replaced.
	 */
	private <T extends EObject> void replaceResourceContent(final T model, final T oldModel) {
		assert model.getClass().equals(oldModel.getClass()) : "Type of old and new model do not match.";

		final Resource r = this.initModels.getResourceSet().getResource(oldModel.eResource().getURI(), false);
		r.setURI(model.eResource().getURI());
		r.getContents().clear();
		r.getContents().add(model); // uproots model from it's original resource...

	}

	/**
	 * 
	 * Helper for replacing the repository models, as they need more attention than
	 * the other models.
	 * 
	 * Requires, that neither the system is not empty.
	 * 
	 * @param archConfig
	 */
	private void replaceRepositoryModel(final ArchitectureConfiguration archConfig) {

		Repository newRepo = archConfig.getAllocation().getSystem_Allocation().getAssemblyContexts__ComposedStructure()
				.get(0).getEncapsulatedComponent__AssemblyContext().getRepository__RepositoryComponent();

		List<Repository> oldPlatformRepos = this.initModels.getRepositories().stream()
				.filter(r -> r.eResource().getURI().isPlatform()).collect(Collectors.toList());

		if (oldPlatformRepos.size() != 1) {
			throw new IllegalStateException("wrong number of platform repos.");
		}

		replaceResourceContent(newRepo, oldPlatformRepos.get(0));

	}
}
