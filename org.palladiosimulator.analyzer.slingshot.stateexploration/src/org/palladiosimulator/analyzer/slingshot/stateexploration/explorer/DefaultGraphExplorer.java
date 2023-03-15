package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationDriver;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemorySnapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.SpdFactory;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.TargetGroup;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;

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

	final PCMResourceSetPartition initModels;

	private final Map<String, Object> launchConfigurationParams;

	private final DefaultExplorationPlanner blackbox;

	private final RawStateGraph graph;

	private final IProgressMonitor monitor;

	public DefaultGraphExplorer(final PCMResourceSetPartition partition, final SimulationDriver driver,
			final Map<String, Object> launchConfigurationParams, final IProgressMonitor monitor) {
		super();
		this.initModels = partition;
		this.launchConfigurationParams = launchConfigurationParams;
		this.monitor = monitor;

		// TODO
		// cannot yet grab the models at the providers, or can i?
		// get monitor Repo
		final List<EObject> monitors = initModels.getElement(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository());
		if (monitors.size() == 0) {
			throw new IllegalStateException("Monitor not present: List size is 0.");
		}

		this.graph = new DefaultGraph(
				this.createRoot(this.initModels.getAllocation(), (MonitorRepository) monitors.get(0)));

		// TODO
		this.blackbox = new DefaultExplorationPlanner(SpdFactory.eINSTANCE.createSPD(), (DefaultGraph) this.graph);
		//this.blackbox = new DefaultExplorationPlanner(this.initModels.getSpd(), (DefaultGraph) this.graph);

	}

	@Override
	public RawStateGraph start() {
		LOGGER.info("********** DefaultGraphExplorer.start **********");

		for (int i = 0; i < 8; i++) { // just random.
			final SimulationInitConfiguration config = this.blackbox.createConfigForNextSimualtionRun();
			this.exploreBranch(config);
		}
		LOGGER.info("********** DefaultGraphExplorer is done :) **********");
		this.graph.getStates()
				.forEach(s -> LOGGER.info(String.format("%s : %.2f -> %.2f, duration : %.2f,  reason: %s ", s.getId(),
						s.getStartTime(), s.getEndTime(), s.getDuration(), s.getReasonToLeave())));
		this.graph.getTransitions().stream().forEach(t -> LOGGER
				.info(String.format("%s : %.2f type : %s", t.getName(), t.getPointInTime(), t.getType().toString())));
		return this.graph;
	}

	/**
	 * Create root node for the graph. Sadly, ExperimentSettings for root are null
	 * :/
	 */
	private DefaultState createRoot(final Allocation alloc, final MonitorRepository monitoring) {
		final ArchitectureConfiguration rootConfig = new DefaultArchitectureConfiguration(alloc, monitoring);

		final Snapshot initSnapshot = new InMemorySnapshot(Set.of());

		final DefaultState root = new DefaultState(0.0, rootConfig);
		root.setSnapshot(initSnapshot);

		return root;
	}

	/**
	 *
	 * Creates tons of configurations from the given configuration and uses them so
	 * start a simulation run.
	 *
	 * @param config
	 * @throws JobFailedException
	 */
	private void exploreBranch(final SimulationInitConfiguration config) {
		// update provided models
		this.updatePCMPartitionProvider(config.getStateToExplore().getArchitecureConfiguration().getAllocation(),
				config.getStateToExplore().getArchitecureConfiguration().getMonitorRepository());
		// update simucomconfig
		final SimuComConfig simuComConfig = prepareSimuComConfig(config.getStateToExplore()
				.getArchitecureConfiguration().getAllocation().eResource().getURI().toString());
		// ????
		final SnapshotConfiguration snapConfig = createSnapConfig(config.getExplorationDuration(),
				!config.getSnapToInitOn().getEvents().isEmpty());

		// TODO *somehow* get this submodule into the driver, such that it will be provided D:
		final SubModule submodule =  new SubModule(config.getSnapToInitOn(),config.getStateToExplore(), snapConfig);


		final AbstractModule simComConfigProvider = new AbstractModule() {
			@Provides
			public IProgressMonitor monitor() {
				return monitor;
			}

			@Provides
			public SimuComConfig config() {
				return simuComConfig;
			}
		};

		final SimulationDriver driver = Slingshot.getInstance().getSimulationDriver();

		driver.init(Set.of(submodule, simComConfigProvider), simuComConfig);
		driver.start();
	}

	private class SubModule extends AbstractModule {

		private final InMemorySnapshot snapToInitOn;
		private final DefaultState currentPartialState;
		private final SnapshotConfiguration snapshotConfiguration;

		public SubModule(final Snapshot snapToInitOn,
		final DefaultState currentPartialState,
		final SnapshotConfiguration snapshotConfiguration) {
			this.snapToInitOn = (InMemorySnapshot) snapToInitOn;
			this.currentPartialState = currentPartialState;
			this.snapshotConfiguration = snapshotConfiguration;
		}

		// PCM instance and SimuComConfig already provided via other means.
		@Provides
		public InMemorySnapshot snapToInitOn() {
			return snapToInitOn;
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
	 *
	 * @param allocation
	 * @param monitorRepository
	 */
	private void updatePCMPartitionProvider(final Allocation allocation, final MonitorRepository monitorRepository) {

		// TODO : update alloc and monitoring
		final PCMResourceSetPartition newPartition = this.initModels;

		final PCMResourceSetPartitionProvider provider = Slingshot.getInstance()
				.getInstance(PCMResourceSetPartitionProvider.class);
		provider.set(newPartition);
	}

	/**
	 * idk, why did i write this??? o_O
	 *
	 * @param spd
	 * @param env
	 * @return
	 */
	private SPD updateSPD(final SPD spd, final ResourceEnvironment env) {
		for (final TargetGroup tg : spd.getTargetGroups()) {
			if (tg instanceof ElasticInfrastructure) {
				((ElasticInfrastructure) tg).setPCM_ResourceEnvironment(env);
			}
		}
		return spd;
	}
}
