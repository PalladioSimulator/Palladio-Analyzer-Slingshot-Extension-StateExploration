package org.palladiosimulator.analyzer.slingshot.planner.data;

public record SLO(String name, String measuringPointURI, Number lowerThreshold, Number upperThreshold) {
	
}
