package org.palladiosimulator.analyzer.slingshot.planner.runner;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.planner.data.State;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraph;
import org.palladiosimulator.analyzer.slingshot.planner.data.Transition;

public class PlannerRunner {
	private static final Logger LOGGER = Logger.getLogger(PlannerRunner.class.getName());

	private StateGraph graph;

	public PlannerRunner(StateGraph graph) {
		this.graph = graph;
	}

	public void start() {
		startBellmanFord();
		startDijkstra();
		startGreedy();
		startGreedyReverse();
	}
	
	public void startBellmanFord() {
		ArrayList<State> states = graph.getStates();
			
		ArrayList<Double> distances = new ArrayList<Double>(states.size());
		ArrayList<State> parents = new ArrayList<State>(states.size());

		ArrayList<Transition> transitions = new ArrayList<Transition>();
		
		for (State s : states) {
			transitions.addAll(s.getOutTransitions());
		}
		
		LOGGER.info("Planning (Bellman-Ford) - started");
		
		initBellmanFord(graph, distances, parents);
		
		for (int i = 0; i < states.size() - 1; i++) {
			for (Transition t : transitions) {
				int targetIndex = states.indexOf(t.getTarget());
				int sourceIndex = states.indexOf(t.getSource());
				
				double alternativeDistance = distances.get(sourceIndex) + t.getTarget().getUtiltity(); 
				
				if (alternativeDistance > distances.get(targetIndex)) {
					distances.set(targetIndex, alternativeDistance);
					parents.set(targetIndex, t.getSource());
				}
			}
		}
		
		LOGGER.info("Planned Path (Bellman-Ford):");

		int currentMaxIndex = -1;
		double currentMaxDistance = 0;
		
		for (int i = 0; i < states.size(); i++) {
			State current = states.get(i);
			if (current.getOutTransitions().size() < 1) { // only have a look at the leafs
				if (currentMaxIndex == -1 || distances.get(i) > currentMaxDistance) {
					currentMaxDistance = distances.get(i);
					currentMaxIndex = i;
				}
			}
		}
		
		LOGGER.info(states.get(currentMaxIndex).getId());
		LOGGER.info("Distance: " + distances.get(currentMaxIndex));
		LOGGER.info("Path: ");
		State parent = states.get(currentMaxIndex);
		while (parent != null) {
			LOGGER.info("  " + parent.getId());
			LOGGER.info("    StartTime: " + parent.getStartTime());
			LOGGER.info("    Duration: " + parent.getDuration());
			LOGGER.info("    EndTime: " + parent.getEndTime());
			LOGGER.info("    Utility: " + parent.getUtiltity());
			/*parent.getMeasurements().stream().forEach(x -> {
				LOGGER.info("    Measurement: " + x.getName());
				x.stream().forEach(y -> LOGGER.info("      " + y.getTimeStamp() + " " + y.getMeasure()));
			});*/
			
			parent = parents.get(states.indexOf(parent));
		}
		
		LOGGER.info("Planning (Bellman-Ford) - finished");
	}
	
	private void initBellmanFord(StateGraph graph, ArrayList<Double> distances, ArrayList<State> parents) {
		for (int i = 0; i < graph.getStates().size(); i++)
			distances.add(-Double.MAX_VALUE);

		for (int i = 0; i < graph.getStates().size(); i++)
			parents.add(null);

		// setting distance of the root knot to 0
		distances.set(graph.getStates().indexOf(graph.getRoot()), 0.0d);
	}

	public void startDijkstra() {
		ArrayList<State> states = graph.getStates();

		ArrayList<State> knots = graph.getStates();
		ArrayList<Double> distances = new ArrayList<Double>(states.size());
		ArrayList<State> parents = new ArrayList<State>(states.size());

		LOGGER.info("Planning (Dijkstra) - started");

		dijkstraInit(graph, distances, parents);

		while (knots.size() > 0) {
			int index = -1;
			double distance = -Double.MAX_VALUE;
			for (int j = 0; j < distances.size(); j++) {
				int current = knots.indexOf(states.get(j));
				double currentdistance = distances.get(j);
				if (current != -1 && currentdistance > distance) {
					index = j;
					distance = distances.get(j);
				}
			}

			if (index != -1) {
				State u = knots.remove(knots.indexOf(states.get(index))); // remove processed knot

				for (Transition t : states.get(index).getOutTransitions()) {
					if (knots.indexOf(t.getTarget()) != -1) { // check whether the knot is in the processing list
																// (knots)
						dijkstraUpdate(u, t.getTarget(), graph, distances, parents);
					}
				}
			}
		}

		LOGGER.info("Planned Path (Dijkstra):");

		int currentMaxIndex = -1;
		double currentMaxDistance = 0;
		
		for (int i = 0; i < states.size(); i++) {
			State current = states.get(i);
			if (current.getOutTransitions().size() < 1) { // only have a look at the leafs
				if (currentMaxIndex == -1 || distances.get(i) > currentMaxDistance) {
					currentMaxDistance = distances.get(i);
					currentMaxIndex = i;
				}
			}
		}
		
		LOGGER.info(states.get(currentMaxIndex).getId());
		LOGGER.info("Distance: " + distances.get(currentMaxIndex));
		LOGGER.info("Path: ");
		State parent = states.get(currentMaxIndex);
		while (parent != null) {
			LOGGER.info("  " + parent.getId());
			LOGGER.info("    StartTime: " + parent.getStartTime());
			LOGGER.info("    Duration: " + parent.getDuration());
			LOGGER.info("    EndTime: " + parent.getEndTime());
			LOGGER.info("    Utility: " + parent.getUtiltity());
			/*parent.getMeasurements().stream().forEach(x -> {
				LOGGER.info("    Measurement: " + x.getName());
				x.stream().forEach(y -> LOGGER.info("      " + y.getTimeStamp() + " " + y.getMeasure()));
			});*/
			
			parent = parents.get(states.indexOf(parent));
		}
		
		LOGGER.info("Planning (Dijkstra) - finished");
	}

	private void dijkstraInit(StateGraph graph, ArrayList<Double> distances, ArrayList<State> parents) {
		for (int i = 0; i < graph.getStates().size(); i++)
			distances.add(-Double.MAX_VALUE);

		for (int i = 0; i < graph.getStates().size(); i++)
			parents.add(null);

		// setting distance of the root knot to 0
		distances.set(graph.getStates().indexOf(graph.getRoot()), 0.0d);
	}

	private void dijkstraUpdate(State u, State v, StateGraph graph, ArrayList<Double> distances,
			ArrayList<State> parents) {
		int indexU = graph.getStates().indexOf(u);
		int indexV = graph.getStates().indexOf(v);

		double alternative = distances.get(indexU) + v.getUtiltity();

		if (alternative > distances.get(indexV)) {
			distances.set(indexV, alternative);
			parents.set(indexV, u);
		}
	}

	public void startGreedy() {
		ArrayList<State> path = new ArrayList<State>();

		LOGGER.info("Planning (Greedy) - started");

		State current = graph.getRoot();
		double utility = 0;
		while (current != null) {
			path.add(current);
			utility += current.getUtiltity();

			State next = null;
			for (Transition t : current.getOutTransitions()) {
				if (next == null) {
					next = t.getTarget();
				} else if (next.getUtiltity() < t.getTarget().getUtiltity()) {
					next = t.getTarget();
				}
			}
			current = next;
		}

		LOGGER.info("Planned Path (Greedy):");
		LOGGER.info("Distance: " + utility);
		LOGGER.info("Path: ");
		path.stream().forEach(x -> {
			LOGGER.info("  " + x.getId());
			LOGGER.info("    StartTime: " + x.getStartTime());
			LOGGER.info("    Duration: " + x.getDuration());
			LOGGER.info("    EndTime: " + x.getEndTime());
			LOGGER.info("    Utility: " + x.getUtiltity());
			/*x.getMeasurements().stream().forEach(xt -> {
				LOGGER.info("    Measurement: " + xt.getName());
				xt.stream().forEach(y -> LOGGER.info("      " + y.getTimeStamp() + " " + y.getMeasure()));
			});*/
			
		});
		LOGGER.info("Planning (Greedy) - finished");
	}
	
	public void startGreedyReverse() {
		ArrayList<State> states = graph.getStates();
		ArrayList<Double> distances = new ArrayList<Double>(states.size());
		ArrayList<State> parents = new ArrayList<State>(states.size());

		for (int i = 0; i < graph.getStates().size(); i++) {
			distances.add(-Double.MAX_VALUE);
			parents.add(null);
		}
		
		LOGGER.info("Planning (Greedy Reverse) - started");
		LOGGER.info("Planned Path (Greedy Reverse):");

		for (int i = 0; i < states.size(); i++) {
			State current = states.get(i);
			if (current.getOutTransitions().size() < 1) { // only have a look at the leaves				
				State parent = current;
				double utiltiy = 0;

				// default setting for leaf note
				utiltiy += parent.getUtiltity();
				
				while (parent != null) {
					boolean found = false;
					stateLoop:
					for (State p : states) {
						for (Transition t : p.getOutTransitions()) {
							if (t.getTarget().equals(parent)) {
								parents.set(states.indexOf(parent), p);
								parent = p;
								found = true;
								utiltiy += parent.getUtiltity();
								break stateLoop;
							}
						}
					}
					if (!found) // when there is no parent found break the loop
						break;
				}
				distances.set(i, utiltiy);
			}
		}

		int currentMaxIndex = -1;
		double currentMaxDistance = 0;
		
		for (int i = 0; i < states.size(); i++) {
			State current = states.get(i);
			if (current.getOutTransitions().size() < 1) { // only have a look at the leafs
				if (currentMaxIndex == -1 || distances.get(i) > currentMaxDistance) {
					currentMaxDistance = distances.get(i);
					currentMaxIndex = i;
				}
			}
		}
		
		LOGGER.info(states.get(currentMaxIndex).getId());
		LOGGER.info("Distance: " + distances.get(currentMaxIndex));
		LOGGER.info("Path: ");
		State parent = states.get(currentMaxIndex);
		while (parent != null) {
			LOGGER.info("  " + parent.getId());
			LOGGER.info("    StartTime: " + parent.getStartTime());
			LOGGER.info("    Duration: " + parent.getDuration());
			LOGGER.info("    EndTime: " + parent.getEndTime());
			LOGGER.info("    Utility: " + parent.getUtiltity());
			/*parent.getMeasurements().stream().forEach(x -> {
				LOGGER.info("    Measurement: " + x.getName());
				x.stream().forEach(y -> LOGGER.info("      " + y.getTimeStamp() + " " + y.getMeasure()));
			});*/
			
			parent = parents.get(states.indexOf(parent));
		}		
		
		LOGGER.info("Planning (Greedy Reverse) - finished");
	}
}
