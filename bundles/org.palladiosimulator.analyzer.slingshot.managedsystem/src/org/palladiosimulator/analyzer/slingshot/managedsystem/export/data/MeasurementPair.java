package org.palladiosimulator.analyzer.slingshot.managedsystem.export.data;

public record MeasurementPair<T> (double time, T value) {
}