package org.palladiosimulator.analyzer.slingshot.planner.data;

public record SLO(String id, String name, String specificationId, Number lowerThreshold, Number upperThreshold) {
	
}
