package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.converter.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

public class StateExploredMessage extends EventMessage<StateGraphNode> {
	public StateExploredMessage(final StateGraphNode payload) {
		super("StateExplored", payload);
	}
}
