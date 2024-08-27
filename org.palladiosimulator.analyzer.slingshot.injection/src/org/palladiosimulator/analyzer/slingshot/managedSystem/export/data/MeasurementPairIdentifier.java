package org.palladiosimulator.analyzer.slingshot.managedSystem.export.data;

import org.palladiosimulator.edp2.models.ExperimentData.Measurement;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;

/**
 *
 *
 *
 * @author Sarah Stieß
 *
 */
public record MeasurementPairIdentifier(String meausurementSpecId, String measuringpointString,
        String metricDescription) {

    public static MeasurementPairIdentifier of(final Measurement measurement,
            final MeasurementSpecification specification) {

        final MeasuringPoint mp = measurement.getMeasuringType()
            .getMeasuringPoint();
        final MetricDescription md = measurement.getMeasuringType()
            .getMetric();

        return new MeasurementPairIdentifier(specification.getId(), mp.getStringRepresentation(), md.getName());
    }
}

