package org.palladiosimulator.analyzer.slingshot.planner.data;

import java.util.List;

import org.palladiosimulator.analyzer.slingshot.planner.data.Utility.UtilityData;
import org.palladiosimulator.analyzer.slingshot.planner.data.Utility.UtilityType;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.common.base.Objects;

public record StateGraphNode(String id, List<Transition> outTransitions, double startTime, double endTime, List<MeasurementSet> measurements,
		List<SLO> slos, Utility utility, String parentId, ScalingPolicy incomingPolicy) {
	
	public StateGraphNode(String id, List<Transition> outTransitions, double startTime, double endTime, List<MeasurementSet> measurements, List<SLO> slos, String parentId, ScalingPolicy incomingPolicy) {
		this(id, outTransitions, startTime, endTime, measurements, slos, calcUtility(startTime, endTime, measurements, slos), parentId, incomingPolicy);	
	}
	
	public double duration() {
		return endTime - startTime;
	}
	
	
	/**
	 * This calculates the utility of the state.
	 * In the form of "(slo1 - measure1) + (slo2 - measure2)"
	 * In addition to this, the sum is multiplied with the duration of the state.
	 * This balances shorter against longer paths so they are compatible.
	 * @return
	 */
	private static Utility calcUtility(double startTime, double endTime, List<MeasurementSet> measurements, List<SLO> slos) {
			
		var utility = new Utility();
		
		for (SLO slo : slos) {
			MeasurementSet ms = measurements.stream().filter(x -> Objects.equal(x.getSpecificationId(),slo.specificationId())).findFirst().orElse(null);
			if (ms != null) 
				utility.addDataInstance(slo.id(), calculateUtility(slo, ms) * (endTime - startTime), UtilityType.SLO);
		}
		
		// Add fix costs as placeholder until we have real costs 
		utility.addDataInstance("FixCosts", -4.2 * (endTime - startTime), UtilityType.COST);
		
		utility.calculateTotalUtility();
		return utility;
	}
	
	
	private static double calculateUtility(SLO slo, MeasurementSet ms) {
		return slo.upperThreshold().doubleValue() - ms.getAverage();
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StateGraphNode other = (StateGraphNode) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}

