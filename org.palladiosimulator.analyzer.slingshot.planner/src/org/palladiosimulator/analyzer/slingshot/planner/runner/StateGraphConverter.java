package org.palladiosimulator.analyzer.slingshot.planner.runner;

import de.uka.ipd.sdq.stoex.IntLiteral;
import de.uka.ipd.sdq.stoex.DoubleLiteral;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.palladiosimulator.analyzer.slingshot.planner.data.ContainerSpecification;
import org.palladiosimulator.analyzer.slingshot.planner.data.HDDContainerSpecification;
import org.palladiosimulator.analyzer.slingshot.planner.data.LinkSpecification;
import org.palladiosimulator.analyzer.slingshot.planner.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.planner.data.Reason;
import org.palladiosimulator.analyzer.slingshot.planner.data.ReconfigurationChange;
import org.palladiosimulator.analyzer.slingshot.planner.data.ResourceSpecification;
import org.palladiosimulator.analyzer.slingshot.planner.data.SLO;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.pcm.resourceenvironment.CommunicationLinkResourceSpecification;
import org.palladiosimulator.pcm.resourceenvironment.HDDProcessingResourceSpecification;
import org.palladiosimulator.pcm.resourceenvironment.LinkingResource;
import org.palladiosimulator.pcm.resourceenvironment.ProcessingResourceSpecification;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.spd.ScalingPolicy;

public class StateGraphConverter {	
	public static StateGraphNode convertState(RawModelState state, String parentId, ScalingPolicy scalingPolicy) {
		List<SLO> slos = new ArrayList<SLO>();
		List<MeasurementSet> measuremnets = new ArrayList<MeasurementSet>();
		Map<String, Set<ResourceSpecification>> resourceSpecifications = new HashMap<String, Set<ResourceSpecification>>();
		
		/**
		 * The following lines are needed of the Palladio System to load the Monitors. 
		 * This is a workaround because otherwise the reading the monitor of the SLO MeasurmentDescription for the Measuring Point would be null.
		 */
		for (Monitor monitor : state.getArchitecureConfiguration().getMonitorRepository().getMonitors()) {
			System.out.println(monitor.getEntityName());
		}
		
		Set<ResourceSpecification> setResrouceSpecification = new HashSet<ResourceSpecification>();
		
		if (state.getArchitecureConfiguration() != null && state.getArchitecureConfiguration().getAllocation() != null) {
			for (LinkingResource x : state.getArchitecureConfiguration().getAllocation().getTargetResourceEnvironment_Allocation().getLinkingResources__ResourceEnvironment()) {
				CommunicationLinkResourceSpecification lr = x.getCommunicationLinkResourceSpecifications_LinkingResource();
				setResrouceSpecification.add(new LinkSpecification(lr.getId(), ((DoubleLiteral) lr.getLatency_CommunicationLinkResourceSpecification().getExpression()).getValue(), ((IntLiteral) lr.getThroughput_CommunicationLinkResourceSpecification().getExpression()).getValue(), lr.getFailureProbability()));
			}
			for (ResourceContainer x : state.getArchitecureConfiguration().getAllocation().getTargetResourceEnvironment_Allocation().getResourceContainer_ResourceEnvironment()) {
				for (ProcessingResourceSpecification y : x.getActiveResourceSpecifications_ResourceContainer()) {
					setResrouceSpecification.add(new ContainerSpecification(y.getId(), y.getNumberOfReplicas(), ((IntLiteral) y.getProcessingRate_ProcessingResourceSpecification().getExpression()).getValue(), y.getSchedulingPolicy().getEntityName(), y.getMTTR(), y.getMTTF()));
				}
				for (HDDProcessingResourceSpecification y : x.getHddResourceSpecifications()) {
					setResrouceSpecification.add(new HDDContainerSpecification(y.getId(), y.getNumberOfReplicas(), ((IntLiteral) y.getProcessingRate_ProcessingResourceSpecification().getExpression()).getValue(), y.getSchedulingPolicy().getEntityName(), y.getMTTR(), y.getMTTF(), ((DoubleLiteral) y.getWriteProcessingRate().getExpression()).getValue(), ((DoubleLiteral) y.getReadProcessingRate().getExpression()).getValue()));
				}
			}
			resourceSpecifications.put(state.getId(), setResrouceSpecification);
		}
	
		
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
		
		return new StateGraphNode(state.getId(), state.getStartTime(), state.getEndTime(), measuremnets, slos, resourceSpecifications, parentId, scalingPolicy);
	}
	
	public static SLO visitServiceLevelObjective(ServiceLevelObjective slo) {
		return new SLO(slo.getId(), slo.getName(), slo.getMeasurementSpecification().getId(), (Number) slo.getLowerThreshold().getThresholdLimit().getValue(), (Number) slo.getUpperThreshold().getThresholdLimit().getValue());
	}
}
