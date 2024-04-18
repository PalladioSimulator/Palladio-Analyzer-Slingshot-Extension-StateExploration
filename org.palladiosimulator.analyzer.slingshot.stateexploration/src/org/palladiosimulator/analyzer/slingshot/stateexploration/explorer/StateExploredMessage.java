package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.networking.events.EventMessage;
import org.palladiosimulator.analyzer.slingshot.networking.events.Message;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.planner.runner.StateGraphConverter;

public class StateExploredMessage extends EventMessage<StateGraphNode> {
	public StateExploredMessage(StateGraphNode payload) {
		super("StateExplored", payload);
	}
}
