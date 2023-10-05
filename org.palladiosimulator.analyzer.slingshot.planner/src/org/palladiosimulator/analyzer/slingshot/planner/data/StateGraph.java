package org.palladiosimulator.analyzer.slingshot.planner.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StateGraph {
	private StateGraphNode root;
	
	public StateGraph(StateGraphNode root) {
		this.root = root;
	}

	public StateGraphNode getRoot() {
		return root;
	}
	
	public List<StateGraphNode> getStates() {
		List<StateGraphNode> states = new ArrayList<StateGraphNode>();
		
		states.add(this.getRoot());
		states.addAll(visitStates(this.getRoot()));
		
		return states;
	}
	
	private List<StateGraphNode> visitStates(StateGraphNode state) {
		List<StateGraphNode> states = new ArrayList<StateGraphNode>();
		
		states.addAll(state.outTransitions().stream().map(x -> x.getTarget()).collect(Collectors.toList()));
		
		for (StateGraphNode x : state.outTransitions().stream().map(x -> x.getTarget()).collect(Collectors.toList())) {
			states.addAll(visitStates(x));
		}
		
		return states;	
	}
}
