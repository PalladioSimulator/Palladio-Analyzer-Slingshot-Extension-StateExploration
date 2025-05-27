package org.palladiosimulator.analyzer.slingshot.managedsystem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.Link;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.Plan;
import org.palladiosimulator.analyzer.slingshot.managedsystem.events.PlanUpdated;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.PlanCreatedEventMessage;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.data.PlanStepDto;
import org.palladiosimulator.analyzer.slingshot.networking.data.NetworkingConstants;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * @author Sarah Stieß
 */
@OnEvent(when = PlanCreatedEventMessage.class, then = {})
public class InjectionSystemBehaviour implements SystemBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(InjectionSystemBehaviour.class);

    final static int SLEEP_DELAY = 5000;

	private final Link linkToSimulation;

    private final String clientName;


	@Inject
    public InjectionSystemBehaviour(final Link link, final SystemDriver driver, @Named(NetworkingConstants.CLIENT_NAME) final String clientName) {
		this.linkToSimulation = link;
		this.linkToSimulation.setSystem(driver);


        this.clientName = clientName;
	}



    /**
     *
     * @param event
     */
    @Subscribe
    public void onPlannerPlanCreatedEventMessage(final PlanCreatedEventMessage event) {

        linkToSimulation.setExplorationID(event.getExplorationId());

        final Map<Double, List<ScalingPolicy>> map = new HashMap<>();

        for (final PlanStepDto dto : event.getPayload()
            .plan()) {
            map.put(dto.time(), dto.policies());
        } // filter empty map?

        final PlanUpdated request = new PlanUpdated(new Plan(map));

        while (!linkToSimulation.isLinkToSimulationSet()) {
            try {
                LOGGER.info("Wait because Link to Simulation is not yet set.");
                Thread.sleep((long) Math.floor(SLEEP_DELAY));
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        linkToSimulation.postToSimulation(request);
    }
}
