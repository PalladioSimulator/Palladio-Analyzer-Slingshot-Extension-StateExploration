package org.palladiosimulator.analyzer.slingshot.planner.data;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class StateGraph {
	private State root;
	
	public StateGraph(State root) {
		this.root = root;
	}

	public State getRoot() {
		return root;
	}
	
	public ArrayList<State> getStates() {
		ArrayList<State> states = new ArrayList<State>();
		
		states.add(this.getRoot());
		states.addAll(visitStates(this.getRoot()));
		
		return states;
	}
	
	private ArrayList<State> visitStates(State state) {
		ArrayList<State> states = new ArrayList<State>();
		
		states.addAll(state.getOutTransitions().stream().map(x -> x.getTarget()).collect(Collectors.toList()));
		
		for (State x : state.getOutTransitions().stream().map(x -> x.getTarget()).collect(Collectors.toList())) {
			states.addAll(visitStates(x));
		}
		
		return states;	
	}
}
