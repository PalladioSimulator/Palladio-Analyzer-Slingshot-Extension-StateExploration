package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated; // TODO DELETE, for DEUBG only!!
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationDriver;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
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
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.ExplorationPlanner;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.ExplorationConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.AdditionalConfigurationModule;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.workflow.WorkflowConfigurationModule;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.pcm.allocation.AllocationPackage;
import org.palladiosimulator.spd.ScalingPolicy;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * Core Component of the Exploration.
 *
 * Responsible for starting the simulation runs to explore different branches.
 *
 * @author Sarah Stieß
 *
 */
public class DefaultGraphExplorer implements GraphExplorer {

	private static final Logger LOGGER = Logger.getLogger(DefaultGraphExplorer.class.getName());

	/** content changes with each iteration */
	private final PCMResourceSetPartition initModels;

	private final Map<String, Object> launchConfigurationParams;

	private final ExplorationPlanner blackbox;

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

		this.graph = new DefaultGraph(this.createRoot());
		this.blackbox = new ExplorationPlanner(this.graph, this.getMinDuration());

		this.jGraphGraph = new SimpleDirectedGraph<>(RawTransition.class);
	}

	@Override
	public RawStateGraph start() {
		LOGGER.info("********** DefaultGraphExplorer.start **********");

		for (int i = 0; i < this.getMaxIterations(); i++) { // just random.
			LOGGER.warn("********** Iteration " + i + "**********");
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
	private DefaultState createRoot() {
		final ArchitectureConfiguration rootConfig = UriBasedArchitectureConfiguration
				.createRootArchConfig(this.initModels.getResourceSet());
		final Snapshot initSnapshot = new InMemorySnapshot(Set.of());

		final DefaultState root = new DefaultState(0.0, rootConfig);
		systemDriver.postEvent(new StateExploredMessage(StateGraphConverter.convertState(root, null, null)));
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
		final SimuComConfig simuComConfig = this.prepareSimuComConfig(
				config.getStateToExplore().getArchitecureConfiguration().getSegment(), config.getExplorationDuration());
		// ????
		final SnapshotConfiguration snapConfig = createSnapConfig(config);

		final SimulationDriver driver = Slingshot.getInstance().getSimulationDriver();

		LOGGER.warn("Run on Models at: " + config.getStateToExplore().getArchitecureConfiguration()
				.getUri(AllocationPackage.eINSTANCE.getAllocation()).toString());

		LOGGER.warn("Start with Request to these Resources: ");
		config.getSnapToInitOn().getEvents(this.initModels).stream()
		.filter(e -> e instanceof JobInitiated).map(e -> (JobInitiated) e)
		.filter(e -> e.getEntity() instanceof ActiveJob).map(e -> ((ActiveJob) e.getEntity())
				.getAllocationContext().getResourceContainer_AllocationContext().getId())
		.forEach(id -> LOGGER.info(id));

		LOGGER.warn("start on config" + config.toString());

		WorkflowConfigurationModule.simuComConfigProvider.set(simuComConfig);
		WorkflowConfigurationModule.blackboardProvider.set(blackboard);

		final Set<DESEvent> set = new HashSet<>(config.getSnapToInitOn().getEvents(this.initModels));
		config.getEvent().ifPresent(e -> set.add(e));

		final EventsToInitOnWrapper eventsToInitOn = new EventsToInitOnWrapper(set);

		AdditionalConfigurationModule.defaultStateProvider.set(config.getStateToExplore());
		AdditionalConfigurationModule.snapConfigProvider.set(snapConfig);
		AdditionalConfigurationModule.eventsToInitOnProvider.set(eventsToInitOn);

		driver.init(simuComConfig, monitor);
		driver.start();

		// Post processing :
		final DefaultState current = config.getStateToExplore();

		final ScalingPolicy policy = config.getEvent().isPresent() ? config.getEvent().get().getScalingPolicy() : null;

		systemDriver.postEvent(
				new StateExploredMessage(StateGraphConverter.convertState(current, config.getParentId(), policy)));
		this.blackbox.updateGraphFringePostSimulation(current);

		// reset Additional Configurations
		AdditionalConfigurationModule.reset();
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
	 * @param variation name of the variation.
	 * @param duration  duration of the interval in seconds.
	 * @return {@link SimuComConfig} for the next simulation run.
	 */
	private SimuComConfig prepareSimuComConfig(final String variation, final double duration) {
		// MapHelper.getValue(configuration, VARIATION_ID, String.class)
		launchConfigurationParams.put(SimuComConfig.SIMULATION_TIME, String.valueOf(((long) duration) + 1));
		launchConfigurationParams.put(SimuComConfig.VARIATION_ID, variation);
		launchConfigurationParams.put(SimuComConfig.EXPERIMENT_RUN, this.graph.toString());

		return new SimuComConfig(launchConfigurationParams, true);
	}

	/**
	 * Create a {@link SnapshotConfiguration} required to start a new simulation
	 * run.
	 *
	 * @param config Information from which to build the next
	 *               {@link SnapshotConfiguration}
	 * @return new {@link SnapshotConfiguration}
	 */
	private SnapshotConfiguration createSnapConfig(final SimulationInitConfiguration config) {

		final double interval = config.getExplorationDuration();

		final Set<RawTransition> rootOutEdges = this.graph.outgoingEdgesOf(this.graph.getRoot());
		final boolean notRootSuccesor = rootOutEdges.stream()
				.filter(t -> t.getTarget().equals(config.getStateToExplore())).findAny().isEmpty();

		return new SnapshotConfiguration(interval, notRootSuccesor, 0.5);
	}

	/**
	 * Replace all resources in {@code initModels} with the resources from the
	 * architecture configuration of the upcoming simulation run.
	 *
	 * Currently, {@code PCMResourceSetPartitionProvider} is a singleton, and
	 * already references {@code this.initModels}. Thus, as we only change the
	 * contents of {initModels} we need not update the partition provided by the
	 * partition provider.
	 *
	 * @param config Config for next simulation run
	 */
	private void updatePCMPartitionProvider(final SimulationInitConfiguration config) {
		config.getStateToExplore().getArchitecureConfiguration().transferModelsToSet(this.initModels.getResourceSet());

	}

	/**
	 * Get {@link ExplorationConfiguration.MAX_EXPLORATION_CYCLES} from launch
	 * configuration parameters map.
	 *
	 * @return number of max exploration cycles
	 */
	private int getMaxIterations() {
		final String maxIteration = (String) launchConfigurationParams
				.get(ExplorationConfiguration.MAX_EXPLORATION_CYCLES);

		return Integer.valueOf(maxIteration);
	}

	/**
	 * Get {@link ExplorationConfiguration.MIN_STATE_DURATION} from launch
	 * configuration parameters map.
	 *
	 * @return minimum duration of an exploration cycles
	 */
	private double getMinDuration() {
		final String minDuration = (String) launchConfigurationParams
				.get(ExplorationConfiguration.MIN_STATE_DURATION);

		return Double.valueOf(minDuration);
	}

}
