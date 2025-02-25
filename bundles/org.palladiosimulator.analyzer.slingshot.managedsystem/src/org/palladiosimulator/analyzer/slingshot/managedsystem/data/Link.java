package org.palladiosimulator.analyzer.slingshot.managedsystem.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.events.SystemEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.managedsystem.events.PlanUpdated;


/**
 *
 * A connection to the System
 *
 * @author Sophie Stieß
 *
 */
@Singleton
public class Link {

    private final static Logger LOGGER = Logger.getLogger(Link.class);


    enum Status {DISABLED, UNINITIALISED;}
    private Status status = Status.UNINITIALISED;

	private SimulationScheduling scheduling;
	private SystemDriver system;


    private UUID explorationID;

    public UUID getExplorationID() {
        if (explorationID == null) {
            throw new IllegalStateException("Must not access explorationId, while it is still null.");
        }
        return explorationID;
    }

    public void setExplorationID(final UUID explorationID) {
        this.explorationID = explorationID;
    }

    private boolean planArrived = false;

    public boolean hasPlanArrived() {
        return planArrived;
    }

    final List<ModelAdjustmentRequested> events = new ArrayList<>();

    public void setEvents(final List<ModelAdjustmentRequested> adjustments) {
        if (!this.events.isEmpty()) {
            throw new IllegalStateException("Cannot set new adjustment requested events, while old ones were not yet consumed");
        }
        this.events.addAll(adjustments);
        planArrived = true;
    }

    public List<ModelAdjustmentRequested> getEvents() {
        planArrived = false;
        final List<ModelAdjustmentRequested> copy = new ArrayList<>(events);
        events.clear();
        return copy;
    }

    public boolean isLinkToSystemSet() {
        return this.system != null;
    }

    public boolean isLinkToSimulationSet() {
        return this.scheduling != null;
    }

    public void disable() {
        this.status = Status.DISABLED;
    }


    /**
     *
     * @param event
     */
    public void postToSystem(final SystemEvent event) {
        if (system == null) {
            throw new IllegalStateException(
                    String.format("Cannot schedule event %s because SystemDriver is null.", event.getId()));
        }
		this.system.postEvent(event);
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
        if (this.status == Status.DISABLED) {
            throw new IllegalStateException(
                    String.format("Wont schedule event %s because this Link is disabled. Maybe the simulation is already Finished.", event.getId()));
        }
	    if (this.planArrived) {
	        LOGGER.debug(String.format("Plan already arrived, ignoring PlanUpdated %s", event.getId()));
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
	    if (this.scheduling == null) {
	        this.scheduling = scheduling;
	    } else {
	        throw new IllegalStateException("SimulationScheduling is already set.");
	    }
	}

    /**
     *
     * @param system
     *            system driver to be used.
     */
	public void setSystem(final SystemDriver system) {
        if (this.system == null) {
            this.system = system;
        } else {
            throw new IllegalStateException("SystemDriver is already set.");
        }
	}


}
