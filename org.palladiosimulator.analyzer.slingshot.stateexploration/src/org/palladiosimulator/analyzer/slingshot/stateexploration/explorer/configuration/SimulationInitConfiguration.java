package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;

/**
 *
 *
 * @author stiesssh
 *
 */
public class SimulationInitConfiguration {

	private final Snapshot snapToInitOn;
	private final DefaultState stateToExplore;
	private final double explorationDuration;

	private final Optional<ModelAdjustmentRequested> event;
	private final String parentId;


	public SimulationInitConfiguration(final Snapshot snapToInitOn, final DefaultState stateToExplore,
			final double explorationDuration, final ModelAdjustmentRequested event, final String parentId) {
		super();
		this.snapToInitOn = snapToInitOn;
		this.stateToExplore = stateToExplore;
		this.explorationDuration = explorationDuration;
		this.event = Optional.ofNullable(event);
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

	/**
	 * Get ScalingEvent for reactive reconfiguration transition
	 *
	 * @return the event
	 */
	public Optional<ModelAdjustmentRequested> getEvent() {
		return this.event;
	}

	@Override
	public String toString() {
		return "SimulationInitConfiguration [snapToInitOn=" + snapToInitOn + ", stateToExplore=" + stateToExplore
				+ ", explorationDuration=" + explorationDuration + ", event=" + event + "]";
	}

	public String getParentId() {
		return this.parentId;
	}
}
