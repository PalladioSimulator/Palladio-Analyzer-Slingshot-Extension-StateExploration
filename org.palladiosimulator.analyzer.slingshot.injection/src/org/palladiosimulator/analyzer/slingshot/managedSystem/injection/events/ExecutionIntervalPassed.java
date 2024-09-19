package org.palladiosimulator.analyzer.slingshot.managedSystem.injection.events;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;
import org.palladiosimulator.analyzer.slingshot.managedSystem.injection.data.Plan;

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
