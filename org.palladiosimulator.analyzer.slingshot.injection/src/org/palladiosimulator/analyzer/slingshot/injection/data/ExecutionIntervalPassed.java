package org.palladiosimulator.analyzer.slingshot.injection.data;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

/**
 *
 * TODO
 *
 * @author Sarah Stieß
 *
 */
public class ExecutionIntervalPassed extends AbstractSimulationEvent implements PlanEvent {

    private final Plan plan;

    public ExecutionIntervalPassed(final Plan plan, final double delay) {
        super(delay);
        this.plan = plan;
    }

    @Override
    public Plan getPlan() {
        return plan;
    }

}
