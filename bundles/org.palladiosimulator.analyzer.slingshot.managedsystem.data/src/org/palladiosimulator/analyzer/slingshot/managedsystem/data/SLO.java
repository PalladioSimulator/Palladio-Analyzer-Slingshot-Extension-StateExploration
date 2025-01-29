package org.palladiosimulator.analyzer.slingshot.managedsystem.data;

public record SLO(String id, String name, String specificationId, Number lowerThreshold, Number upperThreshold) {

}
