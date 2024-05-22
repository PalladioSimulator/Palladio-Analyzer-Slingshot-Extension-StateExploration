package org.palladiosimulator.analyzer.slingshot.stateexploration.controller;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.AnnounceGraphExplorerEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.FocusOnStatesEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.IdleTriggerExplorationEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.PruneFringeByTime;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.ReFocusOnStatesEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.ResetExplorerEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.TriggerExplorationEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.networking.messages.TestMessage;

/**
 *
 * can i rely on the order of the system events? i.e. first published, first
 * received?
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when = TestMessage.class)
@OnEvent(when = AnnounceGraphExplorerEvent.class)
@OnEvent(when = TriggerExplorationEvent.class)
@OnEvent(when = IdleTriggerExplorationEvent.class)
@OnEvent(when = FocusOnStatesEvent.class)
@OnEvent(when = ReFocusOnStatesEvent.class)
@OnEvent(when = ResetExplorerEvent.class)
@OnEvent(when = PruneFringeByTime.class)
public class ExplorerControllerSystemBehaviour implements SystemBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(ExplorerControllerSystemBehaviour.class.getName());

	private GraphExplorer explorer = null;

	private State state = State.STOPPED;
	private enum State {
		READY, STOPPED, RUNNING;
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
	public void onAnnounceGraphExplorerEvent(final AnnounceGraphExplorerEvent event) {
		if (explorer == null) {
			this.explorer = event.getExplorer();
			this.state = State.READY;
		}
		// TODO handling if explorer already set
	}

	/**
	 *
	 * @param event
	 */
	@Subscribe
	public void onIdleTrigger(final IdleTriggerExplorationEvent event) {
		if (this.state == State.STOPPED) {
			throw new IllegalStateException("Cannot do graph exploration cycle because controller is State.STOPPED.");
		}

		this.explorer.start();
		Slingshot.getInstance().getSystemDriver().postEvent(new IdleTriggerExplorationEvent());

		LOGGER.warn("********** DefaultGraphExplorer is done :) **********");
		LOGGER.warn("********** States : ");
		this.explorer.getGraph().getStates()
		.forEach(s -> LOGGER.warn(String.format("%s : %.2f -> %.2f, duration : %.2f,  reason: %s ", s.getId(),
				s.getStartTime(), s.getEndTime(), s.getDuration(), s.getReasonToLeave())));
		LOGGER.warn("********** Transitions : ");
		this.explorer.getGraph().getTransitions().stream().forEach(
				t -> LOGGER.warn(String.format("%s : %.2f type : %s", t.getName(), t.getPointInTime(), t.getType())));
	}

	/**
	 *
	 * @param event
	 */
	@Subscribe
	public void onDoGraphExplorationCycle(final TriggerExplorationEvent event) {
		if (this.state == State.STOPPED) {
			throw new IllegalStateException("Cannot do graph exploration cycle because controller is State.STOPPED.");
		}

		for (int i = 0; i < event.getIterations() && this.explorer.hasNext(); i++) {
			this.explorer.start();
		}

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

	}

	@Subscribe
	public void onReFocusOnStatesEvent(final ReFocusOnStatesEvent event) {

	}

	@Subscribe
	public void onResetExplorerEvent(final ResetExplorerEvent event) {
		this.explorer = null;
		// und dann..? wo krieg ich jetzt 'nen neuen explorere her?
	}

	@Subscribe
	public void onPruneFringeByTime(final PruneFringeByTime event) {

	}
}
