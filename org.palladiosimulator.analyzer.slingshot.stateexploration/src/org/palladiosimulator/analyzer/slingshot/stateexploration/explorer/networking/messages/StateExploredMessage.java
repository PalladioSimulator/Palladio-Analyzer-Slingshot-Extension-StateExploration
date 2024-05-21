package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.networking.messages;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;
import org.palladiosimulator.analyzer.slingshot.networking.data.Message;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.planner.runner.StateGraphConverter;

public class StateExploredMessage extends EventMessage<StateGraphNode> {
	public StateExploredMessage(StateGraphNode payload) {
		super("StateExplored", payload);
	}
}
