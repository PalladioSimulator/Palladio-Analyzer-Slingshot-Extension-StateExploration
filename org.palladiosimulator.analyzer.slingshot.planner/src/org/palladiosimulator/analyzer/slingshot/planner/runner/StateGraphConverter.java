package org.palladiosimulator.analyzer.slingshot.planner.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.planner.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.planner.data.Reason;
import org.palladiosimulator.analyzer.slingshot.planner.data.ReconfigurationChange;
import org.palladiosimulator.analyzer.slingshot.planner.data.SLO;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraph;
import org.palladiosimulator.analyzer.slingshot.planner.data.Transition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.EnvironmentChange;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ReactiveReconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;

public class StateGraphConverter {
	public static StateGraph convert(RawStateGraph graph) {
		StateGraph newGraph = new StateGraph(convertState(graph.getRoot(), null, null));

		// set source from null to correct value
	    for (StateGraphNode s : newGraph.states()) {
	    	List<Transition> trans = new ArrayList<Transition>();
	    	trans.addAll(s.outTransitions());
	    	s.outTransitions().clear();
	    	
	    	for (Transition t : trans) {
	    		s.outTransitions().add(new Transition(s, t.target(), t.reason(), t.change()));
	    	}
	    }
	    
	    return newGraph;
	}
	
	static int i = 0;
	
	public static StateGraphNode convertState(RawModelState state, String parentId, ScalingPolicy scalingPolicy) {
		
		List<SLO> slos = new ArrayList<SLO>();
		List<MeasurementSet> measuremnets = new ArrayList<MeasurementSet>();
		List<Transition> transitions = new ArrayList<Transition>();
		
		/**
		 * The following lines are needed of the Palladio System to load the Monitors. 
		 * This is a workaround because otherwise the reading the monitor of the SLO MeasurmentDescription for the Measuring Point would be null.
		 */
		for (Monitor monitor : state.getArchitecureConfiguration().getMonitorRepository().getMonitors()) {
			System.out.println(monitor.getEntityName());
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
			org.palladiosimulator.analyzer.slingshot.planner.data.Change change = null;
			
			if (!transition.getChange().isPresent()) // no change in PCM instance -> measurement change
				reason = Reason.IntervalChange;
			else if (transition.getChange().get() instanceof EnvironmentChange) // environment change
				reason = Reason.EnviromentalChange;
			else if (transition.getChange().get() instanceof Reconfiguration) { // reconfiguration change
				reason = Reason.ReconfigurationChange;
				Optional<Change> changeOrg = transition.getChange();
				if (changeOrg.isPresent())					
					change = new ReconfigurationChange(((Reconfiguration) changeOrg.get()).getScalingPolicy(), transition.getSource().getEndTime());
			} else if (transition.getChange().get() instanceof ReactiveReconfiguration)  // reactive reconfiguration change
				reason = Reason.ReactiveReconfigurationChange;
			
			// source is null to prevent infinity loop
			transitions.add(new Transition(null, convertState(transition.getTarget(), transition.getSource().getId(), null), reason, Optional.ofNullable(change)));
		}
		
		return new StateGraphNode(state.getId(), transitions, state.getStartTime(), state.getEndTime(), measuremnets, slos, parentId, scalingPolicy);
	}
}
