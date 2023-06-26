package org.palladiosimulator.analyzer.slingshot.planner.data;

public class SLO {
	private String name;
	private Number lowerThreshold;
	private Number upperThreshold;
	
	public SLO(String name, Number lowerThreshold, Number upperThreshold) {
		this.name = name;
		this.lowerThreshold = lowerThreshold;
		this.upperThreshold = upperThreshold;
	}
	
	public String getName() {
		return name;
	}
	
	public Number getLowerThreshold() {
		return lowerThreshold;
	}
	
	public Number getUpperThreshold() {
		return upperThreshold;
	}
}
