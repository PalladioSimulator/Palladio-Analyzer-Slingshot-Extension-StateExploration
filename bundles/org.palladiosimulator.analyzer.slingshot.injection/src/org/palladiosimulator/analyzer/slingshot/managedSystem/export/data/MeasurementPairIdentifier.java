package org.palladiosimulator.analyzer.slingshot.managedSystem.export.data;

import org.palladiosimulator.monitorrepository.MeasurementSpecification;

/**
 *
 *
 *
 * @author Sarah Stieß
 *
 */
public record MeasurementPairIdentifier(String metricSpecId) {

    public static MeasurementPairIdentifier of(final MeasurementSpecification specification) {
        return new MeasurementPairIdentifier(specification.getId());
    }
}

