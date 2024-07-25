package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.networking;

import org.palladiosimulator.analyzer.slingshot.core.events.PreSimulationConfigurationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.messages.SimTestMessage;

// can be deleted.
@OnEvent(when = SimTestMessage.class)
@OnEvent(when = PreSimulationConfigurationStarted.class)
public class SimulationBehaviourReactionTest implements SimulationBehaviorExtension {

}
