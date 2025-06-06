package org.palladiosimulator.analyzer.slingshot.managedsystem.export.data;

import java.util.List;

/**
 * 
 * @author Sarah Stieß
 *
 */
public record IdentifieableMeasurements(MeasurementPairIdentifier identifier, List<MeasurementPair<Number>> values) {

}
