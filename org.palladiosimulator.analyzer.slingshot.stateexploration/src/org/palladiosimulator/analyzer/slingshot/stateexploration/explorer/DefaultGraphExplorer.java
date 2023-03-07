package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.simulation.api.PCMPartitionManager;
import org.palladiosimulator.analyzer.slingshot.simulation.core.SlingshotComponent;
import org.palladiosimulator.analyzer.slingshot.simulation.core.SlingshotModel;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemorySnapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.TargetGroup;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
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

	private final SlingshotModel initModels;
	private final MDSDBlackboard blackboard;

	private final Map<String, Object> launchConfigurationParams;

	private final DefaultExplorationPlanner blackbox;

	private final RawStateGraph graph;

	public DefaultGraphExplorer(final SlingshotModel model, final MDSDBlackboard blackboard,
			final Map<String, Object> launchConfigurationParams) {
		super();
		this.initModels = model;
		this.blackboard = blackboard;
		this.launchConfigurationParams = launchConfigurationParams;

		this.graph = new DefaultGraph(
				this.createRoot(this.initModels.getAllocationModel(), this.initModels.getMonitorRepository()));
		this.blackbox = new DefaultExplorationPlanner(this.initModels.getSpd(), (DefaultGraph) this.graph);
	}

	@Override
	public RawStateGraph start() {
		LOGGER.info("********** DefaultGraphExplorer.start **********");

		for (int i = 0; i < 8; i++) { // just random.
			final SimulationInitConfiguration config = this.blackbox.createConfigForNextSimualtionRun();

			try {
				this.exploreBranch(config);
			} catch (final JobFailedException e) {
				LOGGER.info(String.format("Exploration of branch %s failed ", config.getStateToExplore()
						.getArchitecureConfiguration().getAllocation().eResource().toString()));
				e.printStackTrace();
			}
		}
		LOGGER.info("********** DefaultGraphExplorer is done :) **********");
		this.graph.getStates().forEach(s -> LOGGER.info(String.format("%s : %.2f -> %.2f, duration : %.2f,  reason: %s ", s.getId(), s.getStartTime(), s.getEndTime(), s.getDuration(), s.getReasonToLeave())));
		this.graph.getTransitions().stream().forEach(t -> LOGGER.info(String.format("%s : %.2f type : %s", t.getName(), t.getPointInTime(), t.getType().toString())));
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
	private void exploreBranch(final SimulationInitConfiguration config) throws JobFailedException {
		final SlingshotModel slingshotModel = createSlingshotModel(initModels,
				config.getStateToExplore().getArchitecureConfiguration().getAllocation(),
				config.getStateToExplore().getArchitecureConfiguration().getMonitorRepository());

		final SimuComConfig simuComConfig = prepareSimuComConfig(config.getStateToExplore()
				.getArchitecureConfiguration().getAllocation().eResource().getURI().toString());
		final SnapshotConfiguration snapConfig = createSnapConfig(config.getExplorationDuration(),
				!config.getSnapToInitOn().getEvents().isEmpty());
		final SlingshotComponent component = createSlingshotComponent(slingshotModel, simuComConfig, snapConfig,
				config.getStateToExplore(), config.getSnapToInitOn());

		try {
			component.getSimulation().init();
			component.getSimulation().startSimulation();
		} catch (final Exception e) {
			throw new JobFailedException("Simulation Could Not Be Created", e);
		}
	}

	/**
	 *
	 *
	 * Creates the beast (SlingshotComponent) that provides all the Stuff we want to
	 * get into the simulation for injection.
	 *
	 * @param slingshotModel        Container for PCM instances
	 * @param simuComConfig         Config for Calculators and such
	 * @param snapshotConfiguration Config for Snapshotting
	 * @param currentPartialState   State the run simulates
	 * @param snapToInitOn          Snapshot from previous state
	 * @return
	 */
	private SlingshotComponent createSlingshotComponent(final SlingshotModel slingshotModel,
			final SimuComConfig simuComConfig, final SnapshotConfiguration snapshotConfiguration,
			final DefaultState currentPartialState, final Snapshot snapToInitOn) {
		final SlingshotComponent.Builder builder = SlingshotComponent.builder().withModule(slingshotModel)
				.withModule(new AbstractModule() {

					@Provides
					public InMemorySnapshot snapToInitOn() {
						return (InMemorySnapshot) snapToInitOn;
					}

					@Provides
					public DefaultState builder() {
						return currentPartialState;
					}

					@Provides
					public SimuComConfig config() {
						return simuComConfig;
					}

					@Provides
					public SnapshotConfiguration snapshotConfig() {
						return snapshotConfiguration;
					}

					@Provides
					public PCMPartitionManager partitionManager() {
						return new PCMPartitionManager(DefaultGraphExplorer.this.blackboard);
					}

					@Override
					protected void configure() {
					}

				});
		return builder.build();
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
	 * Create the Slingshot model required to start a new simulation run.
	 *
	 * @param oldModel
	 * @param allocation
	 * @param monitorRepository
	 * @return
	 */
	private SlingshotModel createSlingshotModel(final SlingshotModel oldModel, final Allocation allocation,
			final MonitorRepository monitorRepository) {
		final SlingshotModel model = SlingshotModel.builder().withAllocationModel(allocation)
				.withUsageModel(oldModel.getUsageModel()).withMonitorinRepositoryFile(monitorRepository)
				.withSpdFile(updateSPD(oldModel.getSpd(), allocation.getTargetResourceEnvironment_Allocation()))
				.build();

		return model;
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
