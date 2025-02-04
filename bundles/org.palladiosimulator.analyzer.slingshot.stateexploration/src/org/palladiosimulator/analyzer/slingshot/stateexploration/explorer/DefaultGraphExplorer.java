package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.converter.StateGraphConverter;
import org.palladiosimulator.analyzer.slingshot.converter.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationDriver;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.TransitionType;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration.SimulationInitConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration.UriBasedArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.Postprocessor;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.Preprocessor;
import org.palladiosimulator.analyzer.slingshot.stateexploration.messages.StateExploredEventMessage;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.AdditionalConfigurationModule;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraphFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.PlannedTransition;
import org.palladiosimulator.analyzer.slingshot.workflow.WorkflowConfigurationModule;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.spd.ScalingPolicy;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * Core Component of the Exploration.
 *
 * Responsible for exploring new states.
 *
 * @author Sarah Stieß
 *
 */
public class DefaultGraphExplorer implements GraphExplorer {

	private static final Logger LOGGER = Logger.getLogger(DefaultGraphExplorer.class.getName());

	/** content changes with each iteration */
	private final PCMResourceSetPartition initModels;

	private final Map<String, Object> launchConfigurationParams;

	private final Preprocessor preprocessor;
	private final Postprocessor postprocessor;

	private final DefaultGraph graph;
	private final DefaultGraphFringe fringe;

	private final IProgressMonitor monitor;

	private final SystemDriver systemDriver = Slingshot.getInstance().getSystemDriver();

	private final MDSDBlackboard blackboard;

	private final int horizonLength;

	public DefaultGraphExplorer(final Map<String, Object> launchConfigurationParams, final IProgressMonitor monitor,
			final MDSDBlackboard blackboard) {
		super();
		this.initModels = (PCMResourceSetPartition) blackboard
				.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);
		this.launchConfigurationParams = launchConfigurationParams;
		this.monitor = monitor;
		this.blackboard = blackboard;
		this.horizonLength = LaunchconfigAccess.getHorizon(launchConfigurationParams);

		EcoreUtil.resolveAll(initModels.getResourceSet());

		this.graph = new DefaultGraph(UriBasedArchitectureConfiguration
					.createRootArchConfig(this.initModels.getResourceSet(), LaunchconfigAccess.getModelLocation(launchConfigurationParams)));

		this.fringe = new DefaultGraphFringe();

		systemDriver.postEvent(
				new StateExploredEventMessage(StateGraphConverter.convertState(this.graph.getRoot(), null, null)));

		this.preprocessor = new Preprocessor(this.graph, this.fringe, LaunchconfigAccess.getMinDuration(launchConfigurationParams));
		this.postprocessor = new Postprocessor(this.graph, this.fringe);
	}

	@Override
	public void exploreNextState() {
		LOGGER.info("********** DefaultGraphExplorer.explore() **********");

		final Optional<SimulationInitConfiguration> config = this.preprocessor.createConfigForNextSimualtionRun();
		config.ifPresent(this::exploreBranch);
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


		LOGGER.debug("Start with Request to these Resources: ");
		config.getSnapToInitOn().getEvents(this.initModels).stream()
		.filter(e -> e instanceof JobInitiated).map(e -> (JobInitiated) e)
		.filter(e -> e.getEntity() instanceof ActiveJob).map(e -> ((ActiveJob) e.getEntity())
				.getAllocationContext().getResourceContainer_AllocationContext().getId())
		.forEach(id -> LOGGER.info(id));

		LOGGER.warn("start on config" + config.toString());

		WorkflowConfigurationModule.simuComConfigProvider.set(simuComConfig);
		WorkflowConfigurationModule.blackboardProvider.set(blackboard);

		
		// create copy here, such that i need not pass the initModels to the update providers.
		final Set<DESEvent> allEvents = new HashSet<>(config.getSnapToInitOn().getEvents(this.initModels));

		AdditionalConfigurationModule.updateProviders(snapConfig, config, allEvents);

		driver.init(simuComConfig, monitor);
		driver.start();

		this.postProcessExplorationCycle(config);
	}

	/**
	 *
	 * Post {@link StateExploredEventMessage}, update fringe and write utility back
	 * to state.
	 *
	 * @param config configuration of exploration cycle to be post processed.
	 */
	private void postProcessExplorationCycle(final SimulationInitConfiguration config) {
		final DefaultState current = config.getStateToExplore();

		final List<ScalingPolicy> policies = config.getAdjustmentEvents().stream().map(e -> e.getScalingPolicy())
				.toList();

		final StateGraphNode node = StateGraphConverter.convertState(current, config.getParentId(), policies);
		current.setUtility(node.utility().getTotalUtilty());

		this.systemDriver.postEvent(new StateExploredEventMessage(node));

		// TODO : this is temporal. remove later on. Actually this is a reasonable idea
		// to include for the prioritazion of the fringe.
		if (current.getEndTime() < this.horizonLength) {
			this.postprocessor.updateGraphFringe(current);
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

		final boolean notRootSuccesor = this.graph.getRoot().getOutgoingTransitions().stream()
				.filter(t -> t.getTarget().equals(config.getStateToExplore())).findAny().isEmpty();

		return new SnapshotConfiguration(interval, notRootSuccesor,
				LaunchconfigAccess.getSensibility(launchConfigurationParams),
				LaunchconfigAccess.getMinDuration(launchConfigurationParams));
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

	

	@Override
	public boolean hasUnexploredChanges() {
		return !this.fringe.isEmpty();
	}

	@Override
	public RawStateGraph getGraph() {
		return this.graph;
	}

	/**
	 * Can currently only refocus state with outgoing transitions.
	 *
	 * @param focusedStates
	 */
	@Override
	public void refocus(final Collection<RawModelState> focusedStates) {

		// find states to be refocused in the graph.

		for (final RawModelState rawModelState : focusedStates) {

			final boolean gotNopped = this.graph.outgoingEdgesOf(rawModelState).stream()
					.anyMatch(t -> t.getType() == TransitionType.NOP);

			final boolean gonnaGetNopped = this.fringe.containsNopTodoFor(rawModelState);

			if (gotNopped || gonnaGetNopped) {
				continue;
			} else {
				this.fringe.add(new PlannedTransition(Optional.empty(), (DefaultState) rawModelState));
			}

		}

		final Predicate<PlannedTransition> pruningCriteria = change -> !focusedStates.contains(change.getStart());

		this.fringe.prune(pruningCriteria);
	}

	@Override
	public void focus(final Collection<RawModelState> focusedStates) {
		final Predicate<PlannedTransition> pruningCriteria = change -> !focusedStates.contains(change.getStart());

		this.fringe.prune(pruningCriteria);
	}

	@Override
	public void pruneByTime(final double time) {
		final Predicate<PlannedTransition> pruningCriteria = change -> change.getStart().getStartTime() < time;

		this.fringe.prune(pruningCriteria);
	}

	private void pruneGraphByTime(final double time) {

		final Set<RawModelState> statesToDelete = this.graph.getStates().stream().filter(s -> s.getEndTime() < time)
				.collect(Collectors.toSet());

		this.graph.removeAllVertices(statesToDelete); // removes only vertexes, and also all edges.

		// TODO : update root.
		// TODO : figure out at which state / branch the managed system currently is

		System.out.println(this.getGraph().getTransitions());
	}
}
