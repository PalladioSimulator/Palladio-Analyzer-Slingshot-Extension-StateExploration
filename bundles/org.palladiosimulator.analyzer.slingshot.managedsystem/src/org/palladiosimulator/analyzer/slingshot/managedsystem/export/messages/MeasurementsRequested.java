package org.palladiosimulator.analyzer.slingshot.managedsystem.export.messages;

import org.palladiosimulator.analyzer.slingshot.managedsystem.export.data.Interval;
import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 *
 * @author Sarah Stieß
 *
 */
public class MeasurementsRequested extends EventMessage<Interval> {

    public static final String MESSAGE_MAPPING_IDENTIFIER = "MeasurementsRequested"; // prone to
                                                                                     // change

    public MeasurementsRequested(final Interval payload, final String creator) {
        super(MESSAGE_MAPPING_IDENTIFIER, payload, creator);
    }

}
