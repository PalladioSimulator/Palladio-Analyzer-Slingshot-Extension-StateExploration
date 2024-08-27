package org.palladiosimulator.analyzer.slingshot.managedSystem.export.messages;

import java.util.List;

import org.palladiosimulator.analyzer.slingshot.managedSystem.export.data.IdentifieableMeasurements;
import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 *
 * @author Sarah Stieß
 *
 */
public class MeasurementsExported extends EventMessage<List<IdentifieableMeasurements>> {

    // prone to change
    public static final String MESSAGE_MAPPING_IDENTIFIER = "MeasurementsExported";

    public MeasurementsExported(final List<IdentifieableMeasurements> payload, final String creator) {
        super(MESSAGE_MAPPING_IDENTIFIER, payload, creator);
    }

}