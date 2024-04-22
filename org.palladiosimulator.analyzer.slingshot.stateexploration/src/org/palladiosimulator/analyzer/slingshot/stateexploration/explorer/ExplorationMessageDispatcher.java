package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import javax.inject.Inject;

import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;



@OnEvent(when = TestMessage.class)
public class ExplorationMessageDispatcher implements SystemBehaviorExtension {
	/**
	 * Proof of concept on how to handle incoming messages.
	 * If gson is setup correctly, the incoming message gets called and dispatched automatically resulting in the subscribers to be called
	 */
	@Subscribe
	public void onStateExplored(TestMessage sim) {
		System.out.println(sim.getPayload());
	}
}
