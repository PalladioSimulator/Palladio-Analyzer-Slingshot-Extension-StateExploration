package org.palladiosimulator.analyzer.slingshot.injection;

import java.util.HashSet;
import java.util.Map;
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
import org.palladiosimulator.analyzer.slingshot.injection.data.Link;
import org.palladiosimulator.analyzer.slingshot.injection.data.Plan;
import org.palladiosimulator.analyzer.slingshot.injection.data.StatesBlackboard;
import org.palladiosimulator.analyzer.slingshot.injection.events.ExecutionIntervalPassed;
import org.palladiosimulator.analyzer.slingshot.injection.events.PlanUpdated;

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
    private final StatesBlackboard states;

    private Plan plan;

    @Inject
    public InjectionSimulationBehaviour(final Link link, final SimulationScheduling scheduling,
            final StatesBlackboard states) {
        this.linkToSystem = link;
        this.linkToSystem.setScheduling(scheduling);

        this.states = states;

        this.plan = new Plan(Map.of());
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
     * Update plan and publish event to trigger first step of the (new) plan.
     *
     * @param event
     *            event holding the new plan.
     * @return event to trigger the first step of the new plan.
     */
    @Subscribe
    public Result<ExecutionIntervalPassed> onPlanUpdated(final PlanUpdated event) {
        this.plan = event.getPlan();
        this.plan.forwardPlanTo(event.time());

        return Result.of(createTriggerForNextStep(event.time()));
    }

    /**
     *
     * Publish {@link ModelAdjustmentRequested} events for all reconfigurations of the triggered
     * step in the plan. If the plan has more steps, also publish an {@link ExecutionIntervalPassed}
     * event to trigger the step after the just triggered step in due time.
     *
     * Requires that this plan has more steps to be executed.
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

        if (this.plan.getPlanSteps()
            .isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Event %s wants to trigger next step of plan %s which has no more steps to be executed",
                    event.getId(), plan.getId()));
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

        events.addAll(this.plan.executeNextStep());
        events.addAll(createTriggerForNextStep(event.time()));

        this.states.cleanUp(event.time()); // what if plan comes in, that references old states, but
                                           // i
                                           // already cleaned them up?!

        return Result.of(events);
    }

    /**
     *
     * @param pointInTime
     *            current time of the simulation
     * @return
     */
    private Set<ExecutionIntervalPassed> createTriggerForNextStep(final double pointInTime) {
        if (!this.plan.isEmpty()) {
            final double delay = this.plan.getTimeOfNextStep() - pointInTime;

            return Set.of(new ExecutionIntervalPassed(this.plan, delay));

        } else {
            LOGGER.info(String.format("Plan %s is empty. No new events will be scheduled.", this.plan.getId()));
            return Set.of();
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
