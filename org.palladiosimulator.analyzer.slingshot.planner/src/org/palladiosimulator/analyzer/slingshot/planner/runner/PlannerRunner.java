package org.palladiosimulator.analyzer.slingshot.planner.runner;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
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
		// setting up a FileAppender dynamically...
		SimpleLayout layout = new SimpleLayout();
		FileAppender appender = null;
		try {
			appender = new FileAppender(layout,"/tmp/state_graph_graphical_representation.log",false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOGGER.addAppender(appender);
		
		startBellmanFord();
		startDijkstra();
		startGreedy();
		startGreedyReverse();
		
		LOGGER.removeAppender(appender);
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
				
				if ((distances.get(sourceIndex) + t.getTarget().getUtiltity()) > distances.get(targetIndex)) {
					distances.set(targetIndex, distances.get(sourceIndex) + t.getTarget().getUtiltity());
					parents.set(targetIndex, t.getSource());
				}
			}
		}
		
		LOGGER.info("Planned Path (Bellman-Ford):");

		for (int i = 0; i < states.size(); i++) {
			State current = states.get(i);
			if (current.getOutTransitions().size() < 1) { // only have a look at the leaves
				LOGGER.info(current.getId());
				LOGGER.info("Distance: " + distances.get(i));
				LOGGER.info("Path: ");
				State parent = current;
				while (parent != null) {
					LOGGER.info("  " + parent.getId());
					parent = parents.get(states.indexOf(parent));
				}
			}
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

				LOGGER.info("Planning: " + u.getId());

				for (Transition t : states.get(index).getOutTransitions()) {
					if (knots.indexOf(t.getTarget()) != -1) { // check whether the knot is in the processing list
																// (knots)
						dijkstraUpdate(u, t.getTarget(), graph, distances, parents);
					}
				}
			}
		}

		LOGGER.info("Planned Path (Dijkstra):");

		for (int i = 0; i < states.size(); i++) {
			State current = states.get(i);
			if (current.getOutTransitions().size() < 1) { // only have a look at the leaves
				LOGGER.info(current.getId());
				LOGGER.info("Distance: " + distances.get(i));
				LOGGER.info("Path: ");
				State parent = current;
				while (parent != null) {
					LOGGER.info("  " + parent.getId());
					parent = parents.get(states.indexOf(parent));
				}
			}
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
		while (current != null) {
			path.add(current);

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
		path.stream().forEach(x -> LOGGER.info(x.getId()));
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
				//LOGGER.info(current.getId());
				//LOGGER.info("Path: ");
				
				State parent = current;
				double utiltiy = 0;

				// default setting for leaf note
				utiltiy += parent.getUtiltity();
				//LOGGER.info("  " + parent.getId());
				
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
								//LOGGER.info("  " + parent.getId());
								break stateLoop;
							}
						}
					}
					if (!found) // when there is no parent found break the loop
						break;
				}
				distances.set(i, utiltiy);
				//LOGGER.info("Distance: " + utiltiy);
			}
		}

		int currentMaxIndex = 0;
		double currentMaxDistance = 0;
		
		for (int i = 0; i < states.size(); i++) {
			State current = states.get(i);
			if (current.getOutTransitions().size() < 1) { // only have a look at the leaves
				if (distances.get(i) > currentMaxDistance) {
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
			parent = parents.get(states.indexOf(parent));
		}		
		
		LOGGER.info("Planning (Greedy Reverse) - finished");
	}
}
