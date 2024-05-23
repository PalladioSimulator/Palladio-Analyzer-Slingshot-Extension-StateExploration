package org.palladiosimulator.analyzer.slingshot.planner.data.events;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;
import org.palladiosimulator.analyzer.slingshot.networking.data.Message;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.planner.runner.StateGraphConverter;

public class StateExploredEventMessage extends EventMessage<StateGraphNode> {
	public StateExploredEventMessage(StateGraphNode payload) {
		super("StateExplored", payload);
	}
}
