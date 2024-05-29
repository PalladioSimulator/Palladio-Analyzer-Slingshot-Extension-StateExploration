package org.palladiosimulator.analyzer.slingshot.injection;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.injection.data.ExecutionIntervalPassed;
import org.palladiosimulator.analyzer.slingshot.injection.data.Link;
import org.palladiosimulator.analyzer.slingshot.injection.data.Plan;
import org.palladiosimulator.analyzer.slingshot.injection.data.PlanUpdated;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * Behaviour to inject reconfigurations into a simulation run according to a provided plan.
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when = ExecutionIntervalPassed.class, then = { ExecutionIntervalPassed.class,
        ModelAdjustmentRequested.class }, cardinality = EventCardinality.MANY)
@OnEvent(when = PlanUpdated.class, then = {})
@OnEvent(when = SimulationFinished.class, then = {})
public class InjectionSimulationBehaviour implements SimulationBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(InjectionSimulationBehaviour.class);

    private final Link linkToSystem;
    private Plan plan;

    @Inject
    public InjectionSimulationBehaviour(final Link link, final SimulationScheduling scheduling) {
        this.linkToSystem = link;
        this.linkToSystem.setScheduling(scheduling);

    }

    /**
     * Reset simulation driver, to ensure that no events are posted to the now unused driver.
     */
    @Subscribe
    public void onSimulationFinished(final SimulationFinished event) {
        this.linkToSystem.setScheduling(null);
    }

    /**
     *
     * Update plan and publish event to trigger first step of the plan.
     *
     * @param event
     *            event holding the new plan. All steps of the new plan must be in the future.
     * @return event to trigger the first step of the plan.
     */
    @Subscribe
    public Result<ExecutionIntervalPassed> onPlanUpdated(final PlanUpdated event) {

        if (event.getPlan()
            .getTimeOfNextStep() < event.time()) {
            throw new IllegalArgumentException(String.format(
                    "Reveived plan %p starting with reconfigurations at t=%f, but simulation is already a t=%f",
                    event.getPlan()
                        .getId(),
                    event.getPlan()
                        .getTimeOfNextStep(),
                    event.time()));
        }

        this.plan = event.getPlan();

        final double delay = this.plan.getTimeOfNextStep() - event.time();

        return Result.of(new ExecutionIntervalPassed(this.plan, delay));
    }

    /**
     *
     * Publish {@link ModelAdjustmentRequested} events for all reconfigurations of triggered step in
     * plan. If the plan has more steps, also publish an {@link ExecutionIntervalPassed} event to
     * trigger the step after the just triggered step of the plan in due time.
     *
     * @param event
     *            triggers the next step in the plan.
     * @return {@link ModelAdjustmentRequested} events for all reconfigurations of triggered step in
     *         plan and trigger for the step after that, if needed.
     */
    @Subscribe
    public Result<DESEvent> onExecutionIntervalPassed(final ExecutionIntervalPassed event) {

        if (!this.plan.equals(event.getPlan())) {
            LOGGER.info(String.format("Drop event %s because referenced plan %s is outdated.", event.toString(),
                    event.getPlan()
                        .getId()));
            return Result.of();
        }

        if (event.getPlan()
            .getTimeOfNextStep() != event.time()) {
            throw new IllegalArgumentException(String.format(
                    "Next step in plan %p is at t=%f, but simulation attempted to trigger it at t=%f", event.getPlan()
                        .getId(),
                    event.getPlan()
                        .getTimeOfNextStep(),
                    event.time()));
        }

        final Set<DESEvent> events = new HashSet<>();

        for (final ScalingPolicy policy : this.plan.getReconfigruations(event.time())) {
            events.add(new ModelAdjustmentRequested(policy));
        }

        this.plan.forwardPlanTo(this.plan.getTimeOfNextStep());

        if (!this.plan.isEmpty()) {
            final double delay = this.plan.getTimeOfNextStep() - event.time();
            events.add(new ExecutionIntervalPassed(this.plan, delay));
        } else {
            LOGGER.info(String.format("Plan %s is empty. No new events will be scheduled.", event.getPlan()
                .getId()));
        }

        return Result.of(events);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
