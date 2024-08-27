package org.palladiosimulator.analyzer.slingshot.managedSystem.export.data;

import java.util.HashMap;

import javax.inject.Singleton;

/**
 * TODO : ensure thread safety.
 *
 * @author Sarah Stieß
 *
 */
@Singleton
public final class MeasurementsBlackBoard {

    record Measurement<T> (double time, T value) {
    }

    record Identifier(String id) {

    }

    final HashMap<Identifier, Measurement<? extends Number>> measurementsMap = new HashMap<>();

    final public void addMeasurement(final Object o) {
        // TODO
    }

    final public Object getMeasurements() {
        // TODO
        return null;
    }

}
