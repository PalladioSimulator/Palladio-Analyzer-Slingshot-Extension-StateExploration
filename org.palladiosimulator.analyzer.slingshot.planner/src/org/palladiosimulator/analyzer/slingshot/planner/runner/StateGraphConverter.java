package org.palladiosimulator.analyzer.slingshot.planner.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.palladiosimulator.analyzer.slingshot.planner.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.planner.data.Reason;
import org.palladiosimulator.analyzer.slingshot.planner.data.SLO;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraph;
import org.palladiosimulator.analyzer.slingshot.planner.data.Transition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.EnvironmentChange;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ReactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;

public class StateGraphConverter {
	public static StateGraph convert(RawStateGraph graph) {
		Map<String, StateGraphNode> newStates = new HashMap<String, StateGraphNode>();

		for (RawModelState state : graph.getStates()) {
			List<SLO> slos = new ArrayList<SLO>();
			List<MeasurementSet> measuremnets = new ArrayList<MeasurementSet>();
			List<Transition> transitions = new ArrayList<Transition>();
			
			/**
			 * The following lines are needed of the Palladio System to load the Monitors. 
			 * This is a workaround because otherwise the reading the monitor of the SLO MeasurmentDescription for the Measuring Point would be null.
			 */
			System.out.println("Monitors:");
			for (Monitor monitor : state.getArchitecureConfiguration().getMonitorRepository().getMonitors()) {
				//System.out.println(monitor.getEntityName());
			}
			
			if (state.getArchitecureConfiguration() != null && state.getArchitecureConfiguration().getSLOs() != null) {
				slos = new ArrayList<SLO>();
				
				for (ServiceLevelObjective slo : state.getArchitecureConfiguration().getSLOs().getServicelevelobjectives()) {
					slos.add(PalladioSimulationsVisitor.visitServiceLevelObjective(slo));
				}
			}
			
			// getting all measurements from the RawState and add them to the newly created state
			if (state.getMeasurements() != null) {
				measuremnets = PalladioSimulationsVisitor.visitExperiementSetting(state.getMeasurements());
			}

			// for every transition a path is written to the file
			for (RawTransition transition : state.getOutTransitions()) {
				Reason reason = null;
				if (!transition.getChange().isPresent()) // no change in PCM instance -> measurement change
					reason = Reason.IntervalChange;
				else if (transition.getChange().get() instanceof EnvironmentChange) // environment change
					reason = Reason.EnviromentalChange;
				else if (transition.getChange().get() instanceof Reconfiguration) // reconfiguration change
					reason = Reason.ReconfigurationChange;
				else if (transition.getChange().get() instanceof ReactiveReconfiguration)  // reactive reconfiguration change
					reason = Reason.ReactiveReconfigurationChange;
				
				transitions.add(new Transition(newStates.get(transition.getTarget().getId()),newStates.get(transition.getSource().getId()), reason));
			}
			
			newStates.put(state.getId(), new StateGraphNode(state.getId(), transitions, state.getStartTime(), state.getEndTime(), measuremnets, slos, null));
		}
		
		return new StateGraph(newStates.get(graph.getRoot().getId()));
	}
}
