package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.converter.StateGraphConverter;
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
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.ExplorationPlanner;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.ExplorationConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.messages.StateExploredEventMessage;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.AdditionalConfigurationModule;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultGraphFringe;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ToDoChange;
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

	private final ExplorationPlanner blackbox;

	private final DefaultGraph graph;
	private final DefaultGraphFringe fringe;

	private final IProgressMonitor monitor;

	private final SystemDriver systemDriver = Slingshot.getInstance().getSystemDriver();

	private final MDSDBlackboard blackboard;

	public DefaultGraphExplorer(final Map<String, Object> launchConfigurationParams, final IProgressMonitor monitor,
			final MDSDBlackboard blackboard) {
		super();
		this.initModels = (PCMResourceSetPartition) blackboard
				.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);
		this.launchConfigurationParams = launchConfigurationParams;
		this.monitor = monitor;
		this.blackboard = blackboard;

		EcoreUtil.resolveAll(initModels.getResourceSet());

		final Optional<URI> modelLocation = this.getModelLocation();
		if (modelLocation.isPresent()) {
			this.graph = new DefaultGraph(UriBasedArchitectureConfiguration
					.createRootArchConfig(this.initModels.getResourceSet(), modelLocation.get()));
		} else {
			this.graph = new DefaultGraph(UriBasedArchitectureConfiguration
					.createRootArchConfig(this.initModels.getResourceSet()));
		}
		this.fringe = new DefaultGraphFringe();

		systemDriver.postEvent(
				new StateExploredEventMessage(StateGraphConverter.convertState(this.graph.getRoot(), null, null)));

		this.blackbox = new ExplorationPlanner(this.graph, this.fringe, this.getMinDuration());

	}

	@Override
	public void exploreNextState() {
		LOGGER.info("********** DefaultGraphExplorer.explore() **********");

		final SimulationInitConfiguration config = this.blackbox.createConfigForNextSimualtionRun();

		this.exploreBranch(config);
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

		final Set<DESEvent> set = new HashSet<>(config.getSnapToInitOn().getEvents(this.initModels));
		config.getEvent().ifPresent(e -> set.add(e));
		set.addAll(config.getinitializationEvents());

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
				new StateExploredEventMessage(StateGraphConverter.convertState(current, config.getParentId(), policy)));
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

		final boolean notRootSuccesor = this.graph.getRoot().getOutgoingTransitions().stream()
				.filter(t -> t.getTarget().equals(config.getStateToExplore())).findAny().isEmpty();

		return new SnapshotConfiguration(interval, notRootSuccesor, this.getSensibility(), this.getMinDuration());
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
	 * Get {@link ExplorationConfiguration#MAX_EXPLORATION_CYCLES} from launch
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
	 * Get {@link ExplorationConfiguration#MIN_STATE_DURATION} from launch
	 * configuration parameters map.
	 *
	 * @return minimum duration of an exploration cycles
	 */
	private double getMinDuration() {
		final String minDuration = (String) launchConfigurationParams
				.get(ExplorationConfiguration.MIN_STATE_DURATION);

		return Double.valueOf(minDuration);
	}

	/**
	 *
	 *
	 * Get {@link ExplorationConfiguration#SENSIBILITY} from launch configuration
	 * parameters map.
	 *
	 * @return sensibility for stopping regarding SLOs.
	 */
	private double getSensibility() {
		final String minDuration = (String) launchConfigurationParams
				.get(ExplorationConfiguration.SENSIBILITY);

		return Double.valueOf(minDuration);
	}

	/**
	 *
	 *
	 * Get {@link ExplorationConfiguration#MODEL_LOCATION} from launch configuration
	 * parameters map.
	 *
	 * @return sensibility for stopping regarding SLOs.
	 */
	private Optional<URI> getModelLocation() {
		final String modelLocation = (String) launchConfigurationParams
				.get(ExplorationConfiguration.MODEL_LOCATION);

		if (modelLocation.isBlank()) {
			Optional.empty();
		}

		final URI uri = URI.createURI(modelLocation);

		if (!uri.isFile() && uri.hasAbsolutePath()) {
			return Optional.of(URI.createFileURI(modelLocation));
		}

		return Optional.of(URI.createURI(modelLocation));
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
				this.fringe.add(new ToDoChange(Optional.empty(), (DefaultState) rawModelState));
			}

		}

		final Predicate<ToDoChange> pruningCriteria = change -> !focusedStates.contains(change.getStart());

		this.fringe.prune(pruningCriteria);
	}

	@Override
	public void focus(final Collection<RawModelState> focusedStates) {
		final Predicate<ToDoChange> pruningCriteria = change -> !focusedStates.contains(change.getStart());

		this.fringe.prune(pruningCriteria);
	}

	@Override
	public void pruneByTime(final double time) {
		final Predicate<ToDoChange> pruningCriteria = change -> change.getStart().getStartTime() < time;

		this.fringe.prune(pruningCriteria);
	}

}
