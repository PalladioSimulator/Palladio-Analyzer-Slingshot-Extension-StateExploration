package org.palladiosimulator.analyzer.slingshot.managedsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.Link;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.SimplifiedPlanCreatedEventMessage;
import org.palladiosimulator.analyzer.slingshot.networking.data.NetworkingConstants;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * @author Sarah Stieß
 */
@OnEvent(when = SimplifiedPlanCreatedEventMessage.class, then = {})
public class RLInjectionSystemBehaviour implements SystemBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(RLInjectionSystemBehaviour.class);

    final static int SLEEP_DELAY = 5000;

	private final Link linkToSimulation;

	private final List<ScalingPolicy> policies;


	@Inject
    public RLInjectionSystemBehaviour(final Link link, final SystemDriver driver, @Named(NetworkingConstants.CLIENT_NAME) final String clientName, final SPD spd) {
		this.linkToSimulation = link;
		this.linkToSimulation.setSystem(driver);

        this.policies = spd.getScalingPolicies();
	}



    /**
     *
     * @param event
     */
    @Subscribe
    public void onSimplifiedPlanCreatedEventMessage(final SimplifiedPlanCreatedEventMessage event) {

        linkToSimulation.setExplorationID(event.getExplorationId());

        final List<ModelAdjustmentRequested> events = new ArrayList<>();

        for (final String id : event.getPayload()) {
            final Optional<ScalingPolicy> match = policies.stream().filter(policy -> policy.getId().equals(id)).findAny();
            match.ifPresent(m -> events.add(new ModelAdjustmentRequested(m)));
        }

        while (!linkToSimulation.isLinkToSimulationSet()) {
            try {
                LOGGER.info("Wait because Link to Simulation is not yet set.");
                Thread.sleep((long) Math.floor(SLEEP_DELAY));
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        linkToSimulation.setEvents(events);
    }

}
