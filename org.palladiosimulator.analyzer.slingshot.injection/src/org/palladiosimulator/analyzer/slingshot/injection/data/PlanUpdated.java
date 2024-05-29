package org.palladiosimulator.analyzer.slingshot.injection.data;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

/**
 *
 * @author Sarah Stieß
 *
 */
public class PlanUpdated extends AbstractSimulationEvent implements PlanEvent {

    private final Plan plan;

    public PlanUpdated(final Plan plan) {
        super();
        this.plan = plan;
    }

    @Override
    public Plan getPlan() {
        return plan;
    }
}
