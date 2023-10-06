package org.palladiosimulator.analyzer.slingshot.planner.runner;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraphNode;
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
		List<StateGraphNode> states = graph.states();
			
		List<Double> distances = new ArrayList<Double>(states.size());
		List<StateGraphNode> parents = new ArrayList<StateGraphNode>(states.size());

		List<Transition> transitions = new ArrayList<Transition>();
		
		for (StateGraphNode s : states) {
			transitions.addAll(s.outTransitions());
		}
		
		LOGGER.info("Planning (Bellman-Ford) - started");
		
		initBellmanFord(graph, distances, parents);
		
		for (int i = 0; i < states.size() - 1; i++) {
			for (Transition t : transitions) {
				int targetIndex = states.indexOf(t.target());
				int sourceIndex = states.indexOf(t.source());
				
				double alternativeDistance = distances.get(sourceIndex) + t.target().utility(); 
				
				if (alternativeDistance > distances.get(targetIndex)) {
					distances.set(targetIndex, alternativeDistance);
					parents.set(targetIndex, t.source());
				}
			}
		}
		
		LOGGER.info("Planned Path (Bellman-Ford):");

		int currentMaxIndex = -1;
		double currentMaxDistance = 0;
		
		for (int i = 0; i < states.size(); i++) {
			StateGraphNode current = states.get(i);
			if (current.outTransitions().size() < 1) { // only have a look at the leafs
				if (currentMaxIndex == -1 || distances.get(i) > currentMaxDistance) {
					currentMaxDistance = distances.get(i);
					currentMaxIndex = i;
				}
			}
		}
		
		LOGGER.info(states.get(currentMaxIndex).id());
		LOGGER.info("Distance: " + distances.get(currentMaxIndex));
		LOGGER.info("Path: ");
		StateGraphNode parent = states.get(currentMaxIndex);
		while (parent != null) {
			LOGGER.info("  " + parent.id());
			LOGGER.info("    Duration: " + parent.duration());
			LOGGER.info("    Utility: " + parent.utility());
			parent = parents.get(states.indexOf(parent));
		}
		
		LOGGER.info("Planning (Bellman-Ford) - finished");
	}
	
	private void initBellmanFord(StateGraph graph, List<Double> distances, List<StateGraphNode> parents) {
		for (int i = 0; i < graph.states().size(); i++) {
			distances.add(-Double.MAX_VALUE);
			parents.add(null);
		}

		// setting distance of the root knot to 0
		distances.set(graph.states().indexOf(graph.root()), 0.0d);
	}

	public void startDijkstra() {
		List<StateGraphNode> states = graph.states();

		List<StateGraphNode> knots = graph.states();
		List<Double> distances = new ArrayList<Double>(states.size());
		List<StateGraphNode> parents = new ArrayList<StateGraphNode>(states.size());

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
				StateGraphNode u = knots.remove(knots.indexOf(states.get(index))); // remove processed knot

				for (Transition t : states.get(index).outTransitions()) {
					if (knots.indexOf(t.target()) != -1) { // check whether the knot is in the processing list
																// (knots)
						dijkstraUpdate(u, t.target(), graph, distances, parents);
					}
				}
			}
		}

		LOGGER.info("Planned Path (Dijkstra):");

		int currentMaxIndex = -1;
		double currentMaxDistance = 0;
		
		for (int i = 0; i < states.size(); i++) {
			StateGraphNode current = states.get(i);
			if (current.outTransitions().size() < 1) { // only have a look at the leafs
				if (currentMaxIndex == -1 || distances.get(i) > currentMaxDistance) {
					currentMaxDistance = distances.get(i);
					currentMaxIndex = i;
				}
			}
		}
		
		LOGGER.info(states.get(currentMaxIndex).id());
		LOGGER.info("Distance: " + distances.get(currentMaxIndex));
		LOGGER.info("Path: ");
		StateGraphNode parent = states.get(currentMaxIndex);
		while (parent != null) {
			LOGGER.info("  " + parent.id());
			LOGGER.info("    Duration: " + parent.duration());
			LOGGER.info("    Utility: " + parent.utility());
			parent = parents.get(states.indexOf(parent));
		}
		
		LOGGER.info("Planning (Dijkstra) - finished");
	}

	private void dijkstraInit(StateGraph graph, List<Double> distances, List<StateGraphNode> parents) {
		for (int i = 0; i < graph.states().size(); i++) {
			distances.add(-Double.MAX_VALUE);
			parents.add(null);
		}

		// setting distance of the root knot to 0
		distances.set(graph.states().indexOf(graph.root()), 0.0d);
	}

	private void dijkstraUpdate(StateGraphNode u, StateGraphNode v, StateGraph graph, List<Double> distances,
			List<StateGraphNode> parents) {
		int indexU = graph.states().indexOf(u);
		int indexV = graph.states().indexOf(v);

		double alternative = distances.get(indexU) + v.utility();

		if (alternative > distances.get(indexV)) {
			distances.set(indexV, alternative);
			parents.set(indexV, u);
		}
	}

	public void startGreedy() {
		List<StateGraphNode> path = new ArrayList<StateGraphNode>();

		LOGGER.info("Planning (Greedy) - started");

		StateGraphNode current = graph.root();
		double utility = 0;
		while (current != null) {
			path.add(current);
			utility += current.utility();

			StateGraphNode next = null;
			for (Transition t : current.outTransitions()) {
				if (next == null) {
					next = t.target();
				} else if (next.utility() < t.target().utility()) {
					next = t.target();
				}
			}
			current = next;
		}

		LOGGER.info("Planned Path (Greedy):");
		LOGGER.info("Distance: " + utility);
		LOGGER.info("Path: ");
		path.stream().forEach(x -> {
			LOGGER.info("  " + x.id());
			LOGGER.info("    Duration: " + x.duration());
			LOGGER.info("    Utility: " + x.utility());
		});
		LOGGER.info("Planning (Greedy) - finished");
	}
	
	public void startGreedyReverse() {
		List<StateGraphNode> states = graph.states();
		List<Double> distances = new ArrayList<Double>(states.size());
		List<StateGraphNode> parents = new ArrayList<StateGraphNode>(states.size());

		for (int i = 0; i < graph.states().size(); i++) {
			distances.add(-Double.MAX_VALUE);
			parents.add(null);
		}
		
		LOGGER.info("Planning (Greedy Reverse) - started");
		LOGGER.info("Planned Path (Greedy Reverse):");

		for (int i = 0; i < states.size(); i++) {
			StateGraphNode current = states.get(i);
			if (current.outTransitions().size() < 1) { // only have a look at the leaves				
				StateGraphNode parent = current;
				double utiltiy = 0;

				// default setting for leaf note
				utiltiy += parent.utility();
				
				while (parent != null) {
					boolean found = false;
					stateLoop:
					for (StateGraphNode p : states) {
						for (Transition t : p.outTransitions()) {
							if (t.target().equals(parent)) {
								parents.set(states.indexOf(parent), p);
								parent = p;
								found = true;
								utiltiy += parent.utility();
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
			StateGraphNode current = states.get(i);
			if (current.outTransitions().size() < 1) { // only have a look at the leafs
				if (currentMaxIndex == -1 || distances.get(i) > currentMaxDistance) {
					currentMaxDistance = distances.get(i);
					currentMaxIndex = i;
				}
			}
		}
		
		LOGGER.info(states.get(currentMaxIndex).id());
		LOGGER.info("Distance: " + distances.get(currentMaxIndex));
		LOGGER.info("Path: ");
		StateGraphNode parent = states.get(currentMaxIndex);
		while (parent != null) {
			LOGGER.info("  " + parent.id());
			LOGGER.info("    Duration: " + parent.duration());
			LOGGER.info("    Utility: " + parent.utility());
			parent = parents.get(states.indexOf(parent));
		}		
		
		LOGGER.info("Planning (Greedy Reverse) - finished");
	}
}
