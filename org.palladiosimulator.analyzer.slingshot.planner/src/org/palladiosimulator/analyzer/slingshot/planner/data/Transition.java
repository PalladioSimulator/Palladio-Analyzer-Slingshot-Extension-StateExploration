package org.palladiosimulator.analyzer.slingshot.planner.data;

import java.util.Optional;

public record Transition(StateGraphNode source, StateGraphNode target, Reason reason, Optional<Change> change) {
	
}
