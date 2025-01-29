package org.palladiosimulator.analyzer.slingshot.managedsystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.Link;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.Plan;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.StatesBlackboard;
import org.palladiosimulator.analyzer.slingshot.managedsystem.events.PlanUpdated;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.PlanCreatedEventMessage;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.StateExploredEventMessage;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.data.PlanStepDto;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * @author Sarah Stieß
 */
@OnEvent(when = PlanCreatedEventMessage.class, then = {})
@OnEvent(when = StateExploredEventMessage.class, then = {})
public class InjectionSystemBehaviour implements SystemBehaviorExtension {


	private final Link linkToSimulation;
    private final StatesBlackboard states;


	@Inject
    public InjectionSystemBehaviour(final Link link, final SystemDriver driver, final StatesBlackboard states) {
		this.linkToSimulation = link;
		this.linkToSimulation.setSystem(driver);

        this.states = states;
	}



    /**
     *
     * @param event
     */
    @Subscribe
    public void onPlannerPlanCreatedEventMessage(final PlanCreatedEventMessage event) {

        linkToSimulation.setExplorationID(event.getExplorationId());

        final Map<Double, List<ScalingPolicy>> map = new HashMap<>();

        // TODO WTF did i do to my auto formatting?
        for (final PlanStepDto dto : event.getPayload()
            .plan()) {

            final String stateId = dto.stateId()
                .toString();

            final List<ScalingPolicy> policies = states.getPolicies(stateId);

            if (!policies.isEmpty() && !map.containsKey(states.getTime(stateId))) {
                map.put(states.getTime(stateId), new ArrayList<>());
            }

            for (final ScalingPolicy policy : policies) {
                map.get(states.getTime(stateId))
                    .add(policy);

            }
        }

        final PlanUpdated request = new PlanUpdated(new Plan(map));

        linkToSimulation.postToSimulation(request);
    }

    /**
     *
     * @param event
     */
    @Subscribe
    public void onStateExploredEventMessage(final StateExploredEventMessage event) {
        this.states.addState(event.getPayload());
    }
}
