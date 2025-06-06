package org.palladiosimulator.analyzer.slingshot.managedsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PostIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.Link;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.Plan;
import org.palladiosimulator.analyzer.slingshot.managedsystem.events.ExecutionIntervalPassed;
import org.palladiosimulator.analyzer.slingshot.managedsystem.events.PlanUpdated;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.PlanStepAppliedEventMessage;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.PlanStepAppliedEventMessage.PlanStep;
import org.palladiosimulator.analyzer.slingshot.networking.data.NetworkingConstants;

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
@OnEvent(when = SimulationStarted.class, then = {})
public class InjectionSimulationBehaviour implements SimulationBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(InjectionSimulationBehaviour.class);

    private final Link linkToSystem;
    private final String clientName;

    private Plan plan;

    @Inject
    public InjectionSimulationBehaviour(final Link link, final SimulationScheduling scheduling, @Named(NetworkingConstants.CLIENT_NAME) final String clientName) {
        this.linkToSystem = link;
        this.linkToSystem.setScheduling(scheduling);

        this.clientName = clientName;

        this.plan = new Plan(Map.of());
    }

    /**
     * Reset simulation driver, to ensure that no events are posted to the now unused driver.
     */
    @PostIntercept
    public InterceptionResult postInterceptSimulationFinished(final InterceptorInformation interceptionInformation,
            final SimulationFinished event, final Result<?> result) {
        this.linkToSystem.setScheduling(null);
        return InterceptionResult.success();

    }

    /**
     * Announce start of simulation.
     */
    @Subscribe
    public void onSimulationStarted(final SimulationStarted event) {
        // No initital Plan step, as we do not yet know the explorationId.
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
                    "Event %s wants to trigger next step of plan %s, but the plan has no more steps to be executed",
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


        final List<DESEvent> events = new ArrayList<>();
        final List<ModelAdjustmentRequested> adjustments = this.plan.executeNextStep();

        final Set<String> ids = adjustments.stream()
            .map(r -> r.getScalingPolicy()
                .getId())
            .collect(Collectors.toSet());

        this.linkToSystem.postToSystem(PlanStepAppliedEventMessage.of(new PlanStep(event.time(), ids),
                linkToSystem.getExplorationID(), clientName));

        events.addAll(adjustments);
        events.addAll(createTriggerForNextStep(event.time()));

        return Result.of(events);
    }

    /**
     *
     * @param pointInTime
     *            current time of the simulation
     * @return {@link ExecutionIntervalPassed} event for triggering the next step in the plan.
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
