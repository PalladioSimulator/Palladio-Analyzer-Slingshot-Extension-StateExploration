package org.palladiosimulator.analyzer.slingshot.managedSystem.export.messages;

import org.palladiosimulator.analyzer.slingshot.managedSystem.export.data.MeasurementsExportedData;
import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 *
 * @author Sarah Stieß
 *
 */
public class MeasurementsExported extends EventMessage<MeasurementsExportedData> {

    // prone to change
    public static final String MESSAGE_MAPPING_IDENTIFIER = "MeasurementsExported";

    public MeasurementsExported(final MeasurementsExportedData payload, final String creator) {
        super(MESSAGE_MAPPING_IDENTIFIER, payload, creator);
    }

}