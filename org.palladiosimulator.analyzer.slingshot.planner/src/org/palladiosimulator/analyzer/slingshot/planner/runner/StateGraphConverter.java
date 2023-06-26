package org.palladiosimulator.analyzer.slingshot.planner.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.palladiosimulator.analyzer.slingshot.planner.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.planner.data.Reason;
import org.palladiosimulator.analyzer.slingshot.planner.data.SLO;
import org.palladiosimulator.analyzer.slingshot.planner.data.State;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraph;
import org.palladiosimulator.analyzer.slingshot.planner.data.Transition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.EnvironmentChange;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;

public class StateGraphConverter {
	public static StateGraph convert(RawStateGraph graph) {
		StateGraph newGraph;
		Map<String, State> newStates = new HashMap<String, State>();

		for (RawModelState state : graph.getStates())
			newStates.put(state.getId(), new State(state.getId()));

		State newRootState = newStates.get(graph.getRoot().getId());
		newGraph = new StateGraph(newRootState);

		for (RawModelState state : graph.getStates()) {
			State newState = newStates.get(state.getId());
			
			newState.setStartTime(state.getStartTime());
			newState.setEndTime(state.getEndTime());
			
			if (state.getArchitecureConfiguration() != null && state.getArchitecureConfiguration().getSLOs() != null) {
				ArrayList<SLO> slos = new ArrayList<SLO>();
				
				for (ServiceLevelObjective slo : state.getArchitecureConfiguration().getSLOs().getServicelevelobjectives()) {
					slos.add(PalladioSimulationsVisitor.visitServiceLevelObjective(slo));
				}
				
				newState.setSLOs(slos);
			}
			
			// getting all measurements from the RawState and add them to the newly created state
			if (state.getMeasurements() != null) {
				newState.setMeasurements(PalladioSimulationsVisitor.visitExperiementSetting(state.getMeasurements()));
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
				
				newState.addOutTransition(new Transition(newStates.get(transition.getTarget().getId()),newStates.get(transition.getSource().getId()), reason));
			}
		}
		
		return newGraph;
	}
}
