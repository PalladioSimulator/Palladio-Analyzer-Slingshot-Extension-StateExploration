package org.palladiosimulator.analyzer.slingshot.planner.data;

public class SLO {
	private String name;
	private String measuringPointURI;
	private Number lowerThreshold;
	private Number upperThreshold;
	
	public SLO(String name, String resourceURI, Number lowerThreshold, Number upperThreshold) {
		this.name = name;
		this.measuringPointURI = resourceURI;
		this.lowerThreshold = lowerThreshold;
		this.upperThreshold = upperThreshold;
	}
	
	public String getName() {
		return name;
	}
	
	public String getMeasuringPointURI() {
		return measuringPointURI;
	}
	
	public Number getLowerThreshold() {
		return lowerThreshold;
	}
	
	public Number getUpperThreshold() {
		return upperThreshold;
	}
}
