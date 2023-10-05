package org.palladiosimulator.analyzer.slingshot.planner.data;

import java.util.List;

public record StateGraphNode(String id, List<Transition> outTransitions, double startTime, double endTime, List<MeasurementSet> measurements, List<SLO> slos, Double utility) {
	
	public StateGraphNode {
		/**
		 * This calculates the utility of the state.
		 * In the form of "(slo1 - measure1) + (slo2 - measure2)"
		 * In addition to this, the sum is multiplied with the duration of the state.
		 * This balances shorter against longer paths so they are compatible.
		 * @return
		 */
		double value = 0;
		
		for (SLO slo : slos) {
			MeasurementSet ms = measurements.stream().filter(x -> x.getMeasuringPointURI().equals(slo.measuringPointURI())).findFirst().orElse(null);
			if (ms != null)
				value += (slo.upperThreshold().doubleValue() - ms.getMedian());
		}
		
		utility = (endTime - startTime) * value;
	}
	
	public double duration() {
		return endTime - startTime;
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

