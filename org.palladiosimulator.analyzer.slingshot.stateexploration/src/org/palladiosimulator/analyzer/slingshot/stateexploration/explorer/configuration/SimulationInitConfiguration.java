package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration;

import java.util.Collection;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;

/**
 * Configuration that holds all (most?) information for starting a new
 * simulation cycle with the exploration.
 *
 * @author Sarah Stieß
 *
 */
public class SimulationInitConfiguration {

	private final Snapshot snapToInitOn;
	private final DefaultState stateToExplore;
	private final double explorationDuration;

	/** Events that are not part of the simulation, but only for initialising it. */
	private final Collection<DESEvent> initializationEvents;

	private final Set<ModelAdjustmentRequested> event;
	private final String parentId;


	public SimulationInitConfiguration(final Snapshot snapToInitOn, final DefaultState stateToExplore,
			final double explorationDuration, final Set<ModelAdjustmentRequested> event,
			final Collection<DESEvent> initializationEvents, final String parentId) {
		super();
		this.snapToInitOn = snapToInitOn;
		this.stateToExplore = stateToExplore;
		this.explorationDuration = explorationDuration;
		this.event = event;
		this.initializationEvents = initializationEvents;
		this.parentId = parentId;
	}

	public Snapshot getSnapToInitOn() {
		return this.snapToInitOn;
	}
	public DefaultState getStateToExplore() {
		return this.stateToExplore;
	}
	public double getExplorationDuration() {
		return this.explorationDuration;
	}

	public Collection<DESEvent> getinitializationEvents() {
		return this.initializationEvents;
	}

	/**
	 * Get ScalingEvent for reactive reconfiguration transition
	 *
	 * @return the event
	 */
	public Set<ModelAdjustmentRequested> getEvent() {
		return this.event;
	}

	@Override
	public String toString() {
		return "SimulationInitConfiguration [snapToInitOn=" + snapToInitOn + ", stateToExplore=" + stateToExplore
				+ ", explorationDuration=" + explorationDuration + ", events=" + event.stream()
						.map(e -> e.getScalingPolicy().getEntityName() + "[" + e.getScalingPolicy().getId() + "]")
						.reduce("", (a, b) -> a + ", " + b)
				+ "]";
	}

	public String getParentId() {
		return this.parentId;
	}
}
