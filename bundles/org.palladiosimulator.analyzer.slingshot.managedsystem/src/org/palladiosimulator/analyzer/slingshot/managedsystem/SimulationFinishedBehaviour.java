package org.palladiosimulator.analyzer.slingshot.managedsystem;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.ManagedSystemFinishedMessage;
import org.palladiosimulator.analyzer.slingshot.networking.data.NetworkingConstants;

/**
 *
 * Behaviour to post process the end of a simulation.
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = SimulationFinished.class, then = {})
public class SimulationFinishedBehaviour implements SimulationBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(SimulationFinishedBehaviour.class);

    private final String clientName;


    @Inject
    public SimulationFinishedBehaviour(@Named(NetworkingConstants.CLIENT_NAME) final String clientName) {
        this.clientName = clientName;
    }

    /**
     *
     * Publish a message, when simulation finished.
     *
     * @param event
     *            simulation finished event.
     */
    @Subscribe
    public void onSimulationFinished(final SimulationFinished event) {
        Slingshot.getInstance()
            .getSystemDriver()
            .postEvent(new ManagedSystemFinishedMessage(clientName));
    }


    @Override
    public boolean isActive() {
        return true;
    }
}
