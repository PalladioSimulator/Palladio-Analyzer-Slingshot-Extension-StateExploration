package org.palladiosimulator.analyzer.slingshot.stateexploration.controller;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.ExplorationControllerEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.FocusOnStatesEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.PruneFringeByTime;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.ReFocusOnStatesEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.ResetExplorerEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.TriggerExplorationEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.WorkflowJobDone;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.WorkflowJobStarted;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.DefaultGraphExplorer;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.analyzer.workflow.jobs.LoadModelIntoBlackboardJob;
import org.palladiosimulator.analyzer.workflow.jobs.PreparePCMBlackboardPartitionJob;

import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.SequentialBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 *
 * System behavior to control the exploration of the state graph according to
 * received {@link ExplorationControllerEvent} instances.
 *
 * Exploration cycles cannot be interrupted. I.e. the controller waits, until
 * one cycle or batch af explorations is finished and only reacts to the next
 * event after that.
 *
 * Relies on the order of the system events, which should be FIFO.
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when = WorkflowJobStarted.class)
@OnEvent(when = WorkflowJobDone.class)
@OnEvent(when = TriggerExplorationEvent.class)
@OnEvent(when = FocusOnStatesEvent.class)
@OnEvent(when = ReFocusOnStatesEvent.class)
@OnEvent(when = ResetExplorerEvent.class)
@OnEvent(when = PruneFringeByTime.class)
public class ExplorerControllerSystemBehaviour implements SystemBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(ExplorerControllerSystemBehaviour.class.getName());

	private final Lock explorerLock = new ReentrantLock(true);

	private GraphExplorer explorer = null;
	private WorkflowJobStarted initEvent = null;

	private ExplorationState explorationState = ExplorationState.NOTRUNNING;

	private enum ExplorationState {
		RUNNING, NOTRUNNING;
	}

	public ExplorerControllerSystemBehaviour() {

	}

	/**
	 * Creates the explorer, once the workflow job has prepare all the necessary
	 * things, such as the blackboard and the parameters from the launch
	 * configuration.
	 *
	 * @param event
	 */
	@Subscribe
	public void onWorkflowJobStarted(final WorkflowJobStarted event) {
		if (explorationState == ExplorationState.RUNNING) {
			throw new IllegalStateException("Cannot start new explorer because exploration is already running.");
		} else {

			this.explorerLock.lock();
			try {
				this.initEvent = event;
				this.explorer = new DefaultGraphExplorer(this.initEvent.getLaunchConfigurationParams(),
						this.initEvent.getMonitor(), this.initEvent.getBlackboard());
				this.explorationState = ExplorationState.RUNNING;
			} finally {
				this.explorerLock.unlock();
			}
		}
	}

	/**
	 * Reset attributes to initial values such that a new exploration may start.
	 *
	 * Intended for graphical runs. With headless runs it is only ever one
	 * exploration.
	 *
	 * @param event
	 */
	@Subscribe
	public void onWorkflowJobDone(final WorkflowJobDone event) {
		this.explorationState = ExplorationState.NOTRUNNING;
	}

	/**
	 *
	 * @param event
	 */
	@Subscribe
	public void onDoGraphExplorationCycle(final TriggerExplorationEvent event) {
		if (this.explorationState != ExplorationState.RUNNING) {
			throw new IllegalStateException(
					String.format("Cannot Trigger Exploration, Current Explorer is not running, instead it's %s",
							this.explorationState.toString()));
		}

		for (int i = 0; i < event.getIterations() && this.explorer.hasUnexploredChanges(); i++) {
			LOGGER.info("Iteration " + i);
			this.explorerLock.lock();
			try {
				this.explorer.exploreNextState();
			} finally {
				this.explorerLock.unlock();
			}
		}

		logGraph();

		// testFocusHandling();

	}

	/**
	 * DONT USE THIS. TO BE DELETED. I DONT HAVE ANY OTHERWAY TO TEST THE FOCUSING
	 * RIGHT NOW.
	 */
	@Deprecated
	private void testFocusHandling() {
		final Set<RawModelState> someStates = new HashSet<>();
		someStates.addAll(this.explorer.getGraph().getStates());
		someStates.remove(this.explorer.getGraph().getRoot());

		Slingshot.getInstance().getSystemDriver()
		.postEvent(new FocusOnStatesEvent(someStates.stream().map(s -> s.getId()).toList()));

		Slingshot.getInstance().getSystemDriver()
		.postEvent(new ReFocusOnStatesEvent(Set.of(this.explorer.getGraph().getRoot().getId())));
	}

	/**
	 * Logs the graph.
	 */
	private void logGraph() {
		LOGGER.warn("********** DefaultGraphExplorer is done :) **********");
		LOGGER.warn("********** States : ");
		this.explorer.getGraph().getStates()
		.forEach(s -> LOGGER.warn(String.format("%s : %.2f -> %.2f, duration : %.2f,  reason: %s ", s.getId(),
						s.getStartTime(), s.getEndTime(), s.getDuration(), s.getReasonsToLeave())));
		LOGGER.warn("********** Transitions : ");
		this.explorer.getGraph().getTransitions().stream().forEach(
				t -> LOGGER.warn(String.format("%s : %.2f type : %s", t.getName(), t.getPointInTime(), t.getType())));
	}

	@Subscribe
	public void onFocusOnStatesEvent(final FocusOnStatesEvent event) {
		this.explorerLock.lock();
		try {
			this.explorer.focus(this.mapStateIdsToState(event.getFocusStateIds()));
		} finally {
			this.explorerLock.unlock();
		}
	}

	@Subscribe
	public void onReFocusOnStatesEvent(final ReFocusOnStatesEvent event) {
		this.explorerLock.lock();
		try {
			this.explorer.refocus(this.mapStateIdsToState(event.getFocusStateIds()));
		} finally {
			this.explorerLock.unlock();
		}
	}

	@Subscribe
	public void onPruneFringeByTime(final PruneFringeByTime event) {
		this.explorerLock.lock();
		try {
			this.explorer.pruneByTime(event.getCurrentTime());
		} finally {
			this.explorerLock.unlock();
		}
	}

	/**
	 * Reset the explorer, does not care about state of explorer.
	 *
	 * @param event
	 */
	@Subscribe
	public void onResetExplorerEvent(final ResetExplorerEvent event) {
		if (this.initEvent == null) {
			LOGGER.info("Cannot Reset, Workflow not yet started");
			return;
		}

		this.explorerLock.lock();
		try {

			if (this.explorationState == ExplorationState.NOTRUNNING) {
				this.explorer = null;
				return;
			}

			final MDSDBlackboard blackboard = recreatedInitialBlackboard();

			final PCMResourceSetPartitionProvider provider = Slingshot.getInstance()
					.getInstance(PCMResourceSetPartitionProvider.class);
			provider.set((PCMResourceSetPartition) blackboard
					.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID));

			this.explorer = new DefaultGraphExplorer(this.initEvent.getLaunchConfigurationParams(),
					this.initEvent.getMonitor(),
					blackboard);
		} finally {
			this.explorerLock.unlock();
		}
	}

	/**
	 * Create a new blackboard and load the initital models into it.
	 *
	 * @return new blackboard with initial models
	 */
	private MDSDBlackboard recreatedInitialBlackboard() {

		final SequentialBlackboardInteractingJob<MDSDBlackboard> job = new SequentialBlackboardInteractingJob<MDSDBlackboard>();

		job.addJob(new PreparePCMBlackboardPartitionJob());
		this.initEvent.getPcmModelFiles()
		.forEach(modelFile -> LoadModelIntoBlackboardJob.parseUriAndAddModelLoadJob(modelFile, job));

		final MDSDBlackboard newBlackboard = new MDSDBlackboard();
		job.setBlackboard(newBlackboard);

		try {
			job.execute(this.initEvent.getMonitor());
		} catch (JobFailedException | UserCanceledException e) {
			throw new IllegalStateException("Reseting Explorer Failed, cannot continue exploration.", e);
		}

		return newBlackboard;
	}

	/**
	 *
	 * @param stateIds
	 * @return
	 */
	private Collection<RawModelState> mapStateIdsToState(final Collection<String> stateIds) {
		return this.explorer.getGraph().getStates().stream().filter(s -> stateIds.contains(s.getId())).toList();

	}
}
