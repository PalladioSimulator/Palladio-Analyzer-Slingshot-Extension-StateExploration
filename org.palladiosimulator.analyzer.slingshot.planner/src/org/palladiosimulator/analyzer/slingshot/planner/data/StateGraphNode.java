package org.palladiosimulator.analyzer.slingshot.planner.data;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.planner.data.Utility.UtilityData;
import org.palladiosimulator.analyzer.slingshot.planner.data.Utility.UtilityType;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.common.base.Objects;

public record StateGraphNode(String id, double startTime, double endTime, List<MeasurementSet> measurements,
		List<SLO> slos, Utility utility, Map<String, Set<ResourceSpecification>> resourceSpecifications, String parentId, ScalingPolicy incomingPolicy) {
	
	public StateGraphNode(String id, double startTime, double endTime, List<MeasurementSet> measurements, List<SLO> slos, Map<String, Set<ResourceSpecification>> resourceSpecifications, String parentId, ScalingPolicy incomingPolicy) {
		this(id, startTime, endTime, measurements, slos, calcUtility(startTime, endTime, measurements, slos), resourceSpecifications, parentId, incomingPolicy);	
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
	        MeasurementSet ms = measurements.stream()
	        		.filter(x -> x.getSpecificationId().equals(slo.specificationId()))
	        		.findFirst()
	        		.orElse(null);
	        if (ms != null) {
	            double area = calculateSloUtility(ms, slo.lowerThreshold().doubleValue(), slo.upperThreshold().doubleValue());
	            utility.addDataInstance(slo.id(), area, UtilityType.SLO);
	        }
	    }

	    for (var ms : measurements) {
	        if (ms.getMonitorName().startsWith("Cost_")) {
	            double area = calculateAreaUnderCurve(ms);
	            utility.addDataInstance(ms.getMonitorName(), -area, UtilityType.COST);
	        }
	    }

	    utility.calculateTotalUtility();
	    return utility;
	}
	
	
	public static double score(double value, double lowerThreshold, double upperThreshold) {
	    double middle = (lowerThreshold + upperThreshold) / 2.0;
	    double range = upperThreshold - lowerThreshold;
	    double sigma = range / 6.0;  // Adjust sigma for the desired spread of the normal distribution

	    if (value < lowerThreshold) {
	        return lowerThreshold - value;
	    } else if (value > upperThreshold) {
	        return value - upperThreshold;
	    } else {
	        // Calculate the score using a normal distribution centered at the midpoint
	        double exponent = -Math.pow(value - middle, 2) / (2 * Math.pow(sigma, 2));
	        return Math.exp(exponent) * 10;
	    }
	}

	
	private static double calculateSloUtility(MeasurementSet ms, double lowerTreshold, double upperTreshold) {
	    double area = 0.0;
	    var measurements = ms.getElements();

	    // Sort measurements by time to ensure correct integration
	    measurements.sort(Comparator.comparingDouble((x) -> x.timeStamp()));

	    for (int i = 0; i < measurements.size() - 1; i++) {
	        var current = measurements.get(i);
	        var next = measurements.get(i + 1);

            double currentValue = current.measure().doubleValue();
            double nextValue = next.measure().doubleValue();
            
           
            
            area += calculateTrapezoidArea(current.timeStamp(), next.timeStamp(), 
            		score(currentValue, lowerTreshold, upperTreshold), 
            		score(nextValue, lowerTreshold, upperTreshold));
	        
	    }

	    return area;
	}

	private static double calculateAreaUnderCurve(MeasurementSet ms) {
	    double area = 0.0;
	    var measurements = ms.getElements();

	    // Sort measurements by time to ensure correct integration
	    measurements.sort(Comparator.comparingDouble((x) -> x.timeStamp()));

	    for (int i = 0; i < measurements.size() - 1; i++) {
	        var current = measurements.get(i);
	        var next = measurements.get(i + 1);

            double currentValue = current.measure().doubleValue();
            double nextValue = next.measure().doubleValue();
            
            area += calculateTrapezoidArea(current.timeStamp(), next.timeStamp(), currentValue, nextValue);   
	    }

	    return area;
	}

	private static double calculateTrapezoidArea(double startTime, double endTime, double startValue, double endValue) {
	    double width = endTime - startTime;
	    double height = (endValue + startValue);
	    return width * height / 2;
	}

}

