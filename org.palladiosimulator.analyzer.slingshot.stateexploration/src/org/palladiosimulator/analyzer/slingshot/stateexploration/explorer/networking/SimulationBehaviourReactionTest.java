package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.networking;

import javax.inject.Inject;

import org.palladiosimulator.analyzer.slingshot.core.events.PreSimulationConfigurationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.networking.data.SimulationEventBuffer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.messages.SimTestMessage;

// can be deleted.
@OnEvent(when = SimTestMessage.class)
@OnEvent(when = PreSimulationConfigurationStarted.class)
public class SimulationBehaviourReactionTest implements SimulationBehaviorExtension {
	@Inject
	private SimulationEventBuffer simulationEventBuffer;
	@Subscribe
	public void onStateExplored(final SimTestMessage sim) {
		System.out.println(sim.getPayload());
		System.out.print("Success!!!");
		System.exit(0);
	}

	@Subscribe
	public Result<?> onPreSimulationConfigurationStarted(final PreSimulationConfigurationStarted sim) {
		System.out.print("INTERCEPT: "
				+ "PreSimulationConfigurationStarted");
		final var event = simulationEventBuffer.poll();
		if(event == null) {
			return Result.empty();
		}
		return Result.of(event);

	}
}
