package org.palladiosimulator.analyzer.slingshot.planner.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * Simplified version of ArrayList<Measurment<Double>> with the additional method for median and average.
 * 
 * @author Jonas Edlhuber
 *
 */
public class MeasurementSet {
	
	public record Measurement<T>(T measure, double timeStamp) {
		
	}
	
	private List<Measurement<Number>> elements = new ArrayList<>();

	private Double measurementsSetMedian;
	private Double measurementsSetAverage;
	
	private String name;
	private String monitorId;
	private String monitorName;
	private String specificationId;
	private String specificationName;
	private String metricName;
	private String metricDescription;
	
	public MeasurementSet() {
		super();
	}

	public MeasurementSet(Collection<? extends Measurement<Number>> c) {
		this.elements.addAll(c);
	}
	
	public List<Measurement<Number>> getElements() {
		return this.elements;
	}
	
	
	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public String getMetricDescription() {
		return metricDescription;
	}

	public void setMetricDescription(String metricDescription) {
		this.metricDescription = metricDescription;
	}

	public String getSpecificationId() {
		return specificationId;
	}

	public void setSpecificationId(String specificationId) {
		this.specificationId = specificationId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	public String getMonitorId() {
		return monitorId;
	}

	public void setMonitorId(String monitorId) {
		this.monitorId = monitorId;
	}

	public String getMonitorName() {
		return monitorName;
	}

	public void setMonitorName(String monitorName) {
		this.monitorName = monitorName;
	}


	public double getMedian() {
		if (elements.size() < 1) {
			return 0;
		}
		if (measurementsSetMedian == null) {
			double[] doubles = elements.stream().mapToDouble(x -> x.measure().doubleValue()).sorted().toArray();
			measurementsSetMedian = doubles[doubles.length/2];
		}
		return measurementsSetMedian;
	}
	
	public double getAverage() {
		if (elements.size() < 1) {
			return 0;
		}
		if (measurementsSetAverage == null) {
			measurementsSetAverage = elements.stream().mapToDouble(x -> x.measure().doubleValue()).sum()/elements.size();
		}
		return measurementsSetAverage;
	}


	public boolean add(Measurement<Number> e) {
		resetCalcuationCaches();
		return elements.add(e);
	}

	
	private void resetCalcuationCaches() {
		this.measurementsSetAverage = null;
		this.measurementsSetMedian = null;
	}

	public String getSpecificationName() {
		return specificationName;
	}

	public void setSpecificationName(String specificationName) {
		this.specificationName = specificationName;
	}
}
