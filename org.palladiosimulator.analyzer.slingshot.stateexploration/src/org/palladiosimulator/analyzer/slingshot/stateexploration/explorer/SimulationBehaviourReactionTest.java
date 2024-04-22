package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import javax.inject.Inject;

import org.palladiosimulator.analyzer.slingshot.core.events.PreSimulationConfigurationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.networking.util.SimulationEventBuffer;

@OnEvent(when = SimTestMessage.class)
@OnEvent(when = PreSimulationConfigurationStarted.class)
public class SimulationBehaviourReactionTest implements SimulationBehaviorExtension {
	@Inject
	private SimulationEventBuffer simulationEventBuffer;
	@Subscribe
	public void onStateExplored(SimTestMessage sim) {
		System.out.println(sim.getPayload());
		System.out.print("Success!!!");
		System.exit(0);
	}
	
	@Subscribe
	public Result<?> onPreSimulationConfigurationStarted(PreSimulationConfigurationStarted sim) {
		System.out.print("INTERCEPT: "
				+ "PreSimulationConfigurationStarted");
		var event = simulationEventBuffer.poll();
		if(event == null) {
			return Result.empty();
		}
		return Result.of(event);
		
	}
}
