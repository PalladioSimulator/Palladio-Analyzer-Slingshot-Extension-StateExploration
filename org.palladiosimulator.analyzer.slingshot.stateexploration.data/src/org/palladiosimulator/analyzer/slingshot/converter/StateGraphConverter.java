package org.palladiosimulator.analyzer.slingshot.converter;

import java.util.ArrayList;
import java.util.List;

import org.palladiosimulator.analyzer.slingshot.converter.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.converter.data.SLO;
import org.palladiosimulator.analyzer.slingshot.converter.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.spd.ScalingPolicy;

public class StateGraphConverter {	
	public static StateGraphNode convertState(RawModelState state, String parentId, ScalingPolicy scalingPolicy) {
		List<SLO> slos = new ArrayList<SLO>();
		List<MeasurementSet> measuremnets = new ArrayList<MeasurementSet>();
		
		/**
		 * The following lines are needed of the Palladio System to load the Monitors. 
		 * This is a workaround because otherwise the reading the monitor of the SLO MeasurmentDescription for the Measuring Point would be null.
		 */
		for (Monitor monitor : state.getArchitecureConfiguration().getMonitorRepository().getMonitors()) {
			System.out.println(monitor.getEntityName());
		}
		
		// Add SLOs
		if (state.getArchitecureConfiguration() != null && state.getArchitecureConfiguration().getSLOs() != null) {
			slos = new ArrayList<SLO>();
			
			for (ServiceLevelObjective slo : state.getArchitecureConfiguration().getSLOs().getServicelevelobjectives()) {
				slos.add(visitServiceLevelObjective(slo));
			}
		}
		
		
		// Add Measurements
		if (state.getMeasurements() != null) {
			measuremnets = MeasurementConverter.visitExperiementSetting(state.getMeasurements());
		}
		
		return new StateGraphNode(state.getId(), state.getStartTime(), state.getEndTime(), measuremnets, slos, parentId, scalingPolicy);
	}
	
	public static SLO visitServiceLevelObjective(ServiceLevelObjective slo) {
		return new SLO(slo.getId(), slo.getName(), slo.getMeasurementSpecification().getId(), (Number) slo.getLowerThreshold().getThresholdLimit().getValue(), (Number) slo.getUpperThreshold().getThresholdLimit().getValue());
	}
}
