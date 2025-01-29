package org.palladiosimulator.analyzer.slingshot.managedsystem.data;

import java.util.UUID;

import javax.inject.Singleton;

import org.palladiosimulator.analyzer.slingshot.common.events.SystemEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.managedsystem.events.PlanUpdated;


/**
 *
 * A connection to the System
 *
 * @author Sarah Stieß
 *
 */
@Singleton
public class Link {

	private SimulationScheduling scheduling;
	private SystemDriver system;

    private UUID explorationID;

    public UUID getExplorationID() {
        if (explorationID == null) {
            throw new IllegalStateException("Must not access explorationId, while it is still null.");
        }
        return explorationID;
    }

    private boolean planArrived = false;

    public boolean hasPlanArrived() {
        return planArrived;
    }

    public void setExplorationID(final UUID explorationID) {
        this.explorationID = explorationID;
    }

    public void postToSystem(final SystemEvent event) {
		system.postEvent(event);
	}

    /**
     *
     * Post a {@link PlanUpdated} event to the simulation.
     *
     * Also marks, that this link has received an event.
     *
     * @param event
     *            plan updated event to be posted to the simulation driver.
     * @throws IllegalStateException
     *             if the driver is null.
     */
	public void postToSimulation(final PlanUpdated event) {
        if (scheduling == null) {
            throw new IllegalStateException(
                    String.format("Cannot schedule event %s because SimulationScheduling is null.", event.getId()));
        }

		scheduling.scheduleEvent(event);

		planArrived = true;
	}

    /**
     *
     * @param scheduling
     *            simulation driver be used.
     */
	public void setScheduling(final SimulationScheduling scheduling) {
		this.scheduling = scheduling;
	}

    /**
     *
     * @param system
     *            system dirver to be used.
     */
	public void setSystem(final SystemDriver system) {
		this.system = system;
	}


}
