package org.palladiosimulator.analyzer.slingshot.injection;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.injection.data.Link;
import org.palladiosimulator.analyzer.slingshot.injection.events.ExecutionIntervalPassed;
import org.palladiosimulator.analyzer.slingshot.injection.messages.ManagedSystemTimeEventMessage;
import org.palladiosimulator.analyzer.slingshot.networking.data.NetworkingConstants;

/**
 *
 * Publishes event with managed system time every time a step of the plan gets executed, for testing
 * purpose.
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when = ExecutionIntervalPassed.class, then = {})
@OnEvent(when = SimulationStarted.class, then = {})
public class TestBehaviour implements SimulationBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(TestBehaviour.class);

    private final Link linkToSystem;

    private final String clientName;

    @Inject
    public TestBehaviour(final Link link, @Named(NetworkingConstants.CLIENT_NAME) final String clientName) {
        this.linkToSystem = link;
        this.clientName = clientName;
    }

    @Subscribe
    public void preInterceptDESEvent(final SimulationStarted event) {
        this.linkToSystem.postToSystem(new ManagedSystemTimeEventMessage(event.time(), clientName));
    }

    @Subscribe
    public void preInterceptDESEvent(final ExecutionIntervalPassed event) {
        this.linkToSystem.postToSystem(new ManagedSystemTimeEventMessage(event.time(), clientName));
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
