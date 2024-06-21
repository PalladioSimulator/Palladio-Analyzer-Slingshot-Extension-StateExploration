package org.palladiosimulator.analyzer.slingshot.stateexploration.controller;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.ExplorationControllerEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.ExplorerCreated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.FocusOnStatesEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.IdleTriggerExplorationEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.PruneFringeByTime;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.ReFocusOnStatesEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.ResetExplorerEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.TriggerExplorationEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.messages.TestMessage;

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
@OnEvent(when = TestMessage.class)
@OnEvent(when = ExplorerCreated.class)
@OnEvent(when = TriggerExplorationEvent.class)
@OnEvent(when = IdleTriggerExplorationEvent.class)
@OnEvent(when = FocusOnStatesEvent.class)
@OnEvent(when = ReFocusOnStatesEvent.class)
@OnEvent(when = ResetExplorerEvent.class)
@OnEvent(when = PruneFringeByTime.class)
public class ExplorerControllerSystemBehaviour implements SystemBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(ExplorerControllerSystemBehaviour.class.getName());

	private GraphExplorer explorer = null;

	private IdleExploration doIdle = IdleExploration.BLOCKED;

	private enum IdleExploration {
		ONHOLD, BLOCKED, DOING;
	}

	/**
	 *
	 */
	@Subscribe
	public void onTestMessage(final TestMessage sim) {
		System.out.println(sim.getPayload());
		Slingshot.getInstance().getSystemDriver().postEvent(new TriggerExplorationEvent(5));
	}

	@Subscribe
	public void onAnnounceGraphExplorerEvent(final ExplorerCreated event) {
		if (explorer == null) {
			this.explorer = event.getExplorer();
		}
		// TODO handling if explorer already set?
	}

	/**
	 *
	 * @param event
	 */
	@Subscribe
	public void onIdleTrigger(final IdleTriggerExplorationEvent event) {
		if (this.explorer.hasUnexploredChanges()) {
			this.explorer.exploreNextState();
			Slingshot.getInstance().getSystemDriver().postEvent(new IdleTriggerExplorationEvent());
		} else {
			doIdle = IdleExploration.ONHOLD;
			LOGGER.info("No Unexplored Changes, stop Idle exploration.");
		}
	}

	/**
	 *
	 * @param event
	 */
	@Subscribe
	public void onDoGraphExplorationCycle(final TriggerExplorationEvent event) {

		for (int i = 0; i < event.getIterations() && this.explorer.hasUnexploredChanges(); i++) {
			this.explorer.exploreNextState();
		}
		if (doIdle == IdleExploration.ONHOLD) {
			doIdle = IdleExploration.DOING;
			Slingshot.getInstance().getSystemDriver().postEvent(new IdleTriggerExplorationEvent());
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
				s.getStartTime(), s.getEndTime(), s.getDuration(), s.getReasonToLeave())));
		LOGGER.warn("********** Transitions : ");
		this.explorer.getGraph().getTransitions().stream().forEach(
				t -> LOGGER.warn(String.format("%s : %.2f type : %s", t.getName(), t.getPointInTime(), t.getType())));
	}

	@Subscribe
	public void onFocusOnStatesEvent(final FocusOnStatesEvent event) {
		this.explorer.focus(this.mapStateIdsToState(event.getFocusStateIds()));
	}

	@Subscribe
	public void onReFocusOnStatesEvent(final ReFocusOnStatesEvent event) {
		this.explorer.refocus(this.mapStateIdsToState(event.getFocusStateIds()));
	}

	@Subscribe
	public void onPruneFringeByTime(final PruneFringeByTime event) {
		this.explorer.pruneByTime(event.getCurrentTime());
	}

	@Subscribe
	public void onResetExplorerEvent(final ResetExplorerEvent event) {
		this.explorer = null;
		// this.explorer =
		// Slingshot.getInstance().getInstance(DefaultGraphExplorer.class);
		// und dann..? wo krieg ich jetzt 'nen neuen explorere her?
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
