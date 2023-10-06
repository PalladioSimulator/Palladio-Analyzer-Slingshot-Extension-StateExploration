package org.palladiosimulator.analyzer.slingshot.planner.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record StateGraph(StateGraphNode root, List<StateGraphNode> states) {
	public StateGraph(StateGraphNode root) {
		this(root, getStates(root));
	}
	
	private static List<StateGraphNode> getStates(StateGraphNode root) {
		List<StateGraphNode> states = new ArrayList<StateGraphNode>();
		
		states.add(root);
		states.addAll(visitStates(root));
		
		return states;
	}
	
	private static List<StateGraphNode> visitStates(StateGraphNode state) {
		List<StateGraphNode> states = new ArrayList<StateGraphNode>();
		
		states.addAll(state.outTransitions().stream().map(x -> x.target()).collect(Collectors.toList()));
		
		for (StateGraphNode x : state.outTransitions().stream().map(x -> x.target()).collect(Collectors.toList())) {
			states.addAll(visitStates(x));
		}
		
		return states;	
	}
}
