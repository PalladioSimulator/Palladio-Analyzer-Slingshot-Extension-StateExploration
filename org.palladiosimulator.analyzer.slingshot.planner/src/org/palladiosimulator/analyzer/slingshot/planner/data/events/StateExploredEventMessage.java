package org.palladiosimulator.analyzer.slingshot.planner.data.events;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraphNode;

public class StateExploredEventMessage extends EventMessage<StateGraphNode> {

	public static final String MESSAGE_MAPPING_IDENTIFIER = "StateExplored";

	public StateExploredEventMessage(final StateGraphNode payload) {
		super(MESSAGE_MAPPING_IDENTIFIER, payload);
	}
}
