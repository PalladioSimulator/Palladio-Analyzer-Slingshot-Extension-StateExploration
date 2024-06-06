package org.palladiosimulator.analyzer.slingshot.injection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.injection.data.Link;
import org.palladiosimulator.analyzer.slingshot.injection.data.Plan;
import org.palladiosimulator.analyzer.slingshot.injection.data.StatesBlackboard;
import org.palladiosimulator.analyzer.slingshot.injection.events.PlanUpdated;
import org.palladiosimulator.analyzer.slingshot.injection.messages.PlanCreatedEventMessage;
import org.palladiosimulator.analyzer.slingshot.injection.messages.StateExploredEventMessage;
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

        final Map<Double, Set<ScalingPolicy>> map = new HashMap<>();

        for (final String stateId : event.getPayload()) {
            final Optional<ScalingPolicy> policy = states.getPolicy(stateId);
            if (policy.isPresent()) {
                if (!map.containsKey(states.getTime(stateId))) {
                    map.put(states.getTime(stateId), new HashSet<>());
                }
                map.get(states.getTime(stateId))
                    .add(policy.get());
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
