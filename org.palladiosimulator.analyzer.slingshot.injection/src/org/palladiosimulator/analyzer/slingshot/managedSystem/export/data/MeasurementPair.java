package org.palladiosimulator.analyzer.slingshot.managedSystem.export.data;

public record MeasurementPair<T> (double time, T value) {
}