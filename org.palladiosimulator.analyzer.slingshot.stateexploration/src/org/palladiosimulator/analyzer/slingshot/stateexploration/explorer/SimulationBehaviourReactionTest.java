package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;

@OnEvent(when = SimTestMessage.class)
public class SimulationBehaviourReactionTest implements SimulationBehaviorExtension {
	@Subscribe
	public void onStateExplored(SimTestMessage sim) {
		System.out.println(sim.getPayload());
		System.out.print("Success!!!");
		System.exit(0);
	}
}
