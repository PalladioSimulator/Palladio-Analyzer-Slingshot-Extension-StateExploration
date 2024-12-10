package org.palladiosimulator.analyzer.slingshot.injection.utility.converter.triggerevent;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

/**
 *
 * TODO
 *
 * @author Sophie Stieß
 *
 */
public class UtilityIntervalPassed extends AbstractSimulationEvent {


    public UtilityIntervalPassed(final double delay) {
        super(delay);
    }

}
