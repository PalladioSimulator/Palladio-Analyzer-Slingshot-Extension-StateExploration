package org.palladiosimulator.analyzer.slingshot.planner.data;

public record Transition(StateGraphNode source, StateGraphNode target, Reason reason) {
	
}
