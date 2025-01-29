package org.palladiosimulator.analyzer.slingshot.managedsystem.export.data;

import java.util.List;

/**
 *
 * @author Sarah Stieß
 */
public record MeasurementsExportedData(List<IdentifieableMeasurements> measurements, Interval interval) {

}
