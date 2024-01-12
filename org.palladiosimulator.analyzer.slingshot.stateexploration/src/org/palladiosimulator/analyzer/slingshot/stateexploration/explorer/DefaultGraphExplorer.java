package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated; // TODO DELETE, for DEUBG only!!
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationDriver;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.planner.runner.StateGraphConverter;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemorySnapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration.SimulationInitConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration.UriBasedArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.DefaultExplorationPlanner;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.workflow.WorkflowConfigurationModule;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.spd.SPD;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

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

	/** gotta keep this one clean and unchanged! */
	final PCMResourceSetPartition initModels;

	private final Map<String, Object> launchConfigurationParams;

	private final DefaultExplorationPlanner blackbox;

	private final DefaultGraph graph;

	private final IProgressMonitor monitor;
	
	private final SystemDriver systemDriver = Slingshot.getInstance().getSystemDriver();
	
	public DefaultGraphExplorer(final PCMResourceSetPartition partition, final SimulationDriver driver,
			final Map<String, Object> launchConfigurationParams, final IProgressMonitor monitor) {
		super();
		this.initModels = partition;
		this.launchConfigurationParams = launchConfigurationParams;
		this.monitor = monitor;
		
		// TODO
		// cannot yet grab the models at the providers, or can i?
		// get monitor Repo + spds.

		final MonitorRepository monitorRepository = PCMResourcePartitionHelper.getMonitorRepository(partition);
		final SPD spd = PCMResourcePartitionHelper.getSPD(partition);
		final ServiceLevelObjectiveRepository sloRepository = PCMResourcePartitionHelper.getSLORepository(partition);

		this.graph = new DefaultGraph(
				this.createRoot(this.initModels.getAllocation(), monitorRepository, spd, sloRepository));

		this.blackbox = new DefaultExplorationPlanner(spd, this.graph);
	}

	@Override
	public RawStateGraph start() {
		LOGGER.info("********** DefaultGraphExplorer.start **********");

		for (int i = 0; i < 30; i++) { // just random.
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
		this.graph.getTransitions().stream().forEach(t -> LOGGER
				.warn(String.format("%s : %.2f type : %s", t.getName(), t.getPointInTime(), t.getType())));
		return this.graph;
	}

	/**
	 * Create root node for the graph. Sadly, ExperimentSettings for root are null
	 * :/
	 */
	private DefaultState createRoot(final Allocation alloc, final MonitorRepository monitoring, final SPD spd,
			final ServiceLevelObjectiveRepository slo) {
		// final ArchitectureConfiguration rootConfig = new
		// DefaultArchitectureConfiguration(alloc, monitoring, spd);
		final ArchitectureConfiguration rootConfig = UriBasedArchitectureConfiguration.builder().withAllocation(alloc)
				.withMonitorRepository(monitoring).withSPD(spd).withSLO(slo).build();

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
				.getArchitecureConfiguration().getAllocation().eResource().getURI().toString());
		// ????
		final SnapshotConfiguration snapConfig = createSnapConfig(config.getExplorationDuration(),
				!config.getSnapToInitOn().getEvents().isEmpty());

		// TODO *somehow* get this submodule into the driver, such that it will be
		// provided D:
		final SubModule submodule = new SubModule(config, snapConfig);


		WorkflowConfigurationModule.simuComConfigProvider.set(simuComConfig);

		final SimulationDriver driver = Slingshot.getInstance().getSimulationDriver();
		
		LOGGER.info("Run on Models at: " + config.getStateToExplore().getArchitecureConfiguration().getAllocation().eResource().getURI().toString());
		
		
		LOGGER.info("Start with Request to these Resources: ");
		config.getSnapToInitOn().getEvents().stream().filter(e -> e instanceof JobInitiated).map(e -> (JobInitiated) e)
				.filter(e -> e.getEntity() instanceof ActiveJob).map(e -> ((ActiveJob) e.getEntity())
						.getAllocationContext().getResourceContainer_AllocationContext().getId())
				.forEach(id -> LOGGER.info(id));

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
	 * TODO set variation id to something meaningfull, e.g. archConfig x change
	 *
	 * TODO set experiment run to something meaning full
	 *
	 * @param variation
	 * @return
	 */
	private SimuComConfig prepareSimuComConfig(final String variation) {
		// MapHelper.getValue(configuration, VARIATION_ID, String.class)
		launchConfigurationParams.put(SimuComConfig.VARIATION_ID, variation);
		launchConfigurationParams.put(SimuComConfig.EXPERIMENT_RUN, this.graph.toString());

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
	 * It's broken :( --  is it though?
	 *
	 *
	 * @param allocation
	 * @param monitorRepository
	 */
	private void updatePCMPartitionProvider(final SimulationInitConfiguration config) {

		final Allocation allocation = config.getStateToExplore().getArchitecureConfiguration().getAllocation();
		final MonitorRepository monitorRepository = config.getStateToExplore().getArchitecureConfiguration()
				.getMonitorRepository();
		final SPD spd = config.getStateToExplore().getArchitecureConfiguration().getSPD();

		final PCMResourceSetPartition newPartition = this.initModels;
		final ResourceSet rs = newPartition.getResourceSet();

		/* remove old models... */
		// rs.getResources().remove(newPartition.getAllocation().eResource());

		final MonitorRepository currentMonitor = PCMResourcePartitionHelper.getMonitorRepository(initModels);
		// rs.getResources().remove(monitors.get(0).eResource());

		final SPD currentSpd = PCMResourcePartitionHelper.getSPD(initModels);
		// rs.getResources().remove(spds.get(0).eResource());

		/* load new models */
		/*
		 * newPartition.loadModel(allocation.eResource().getURI());
		 * newPartition.loadModel(monitorRepository.eResource().getURI());
		 * newPartition.loadModel(spd.eResource().getURI());
		 */

		{
			final Resource r = rs.getResource(newPartition.getAllocation().eResource().getURI(), false);
			r.getContents().clear();
			r.getContents().add(allocation); // uproots model from it's original resource...
		}
		{
			final Resource r = rs.getResource(currentMonitor.eResource().getURI(), false);
			r.getContents().clear();
			r.getContents().add(monitorRepository); // uproots model from it's original resource...
		}
		{
			final Resource r = rs.getResource(currentSpd.eResource().getURI(), false);
			r.getContents().clear();
			r.getContents().add(spd); // uproots model from it's original resource...
		}

		/* add initial ScalingPolicy, if present */
		if (config.getPolicy().isPresent()) {
			spd.getScalingPolicies().add(config.getPolicy().get());
		}

		final PCMResourceSetPartitionProvider provider = Slingshot.getInstance()
				.getInstance(PCMResourceSetPartitionProvider.class);
		provider.set(newPartition);
	}
}
