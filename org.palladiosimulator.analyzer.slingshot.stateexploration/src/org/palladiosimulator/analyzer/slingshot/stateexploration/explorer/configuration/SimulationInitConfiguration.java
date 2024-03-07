package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
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

	private final Optional<DESEvent> event;

	public SimulationInitConfiguration(final Snapshot snapToInitOn, final DefaultState stateToExplore,
			final double explorationDuration, final DESEvent event) {
		super();
		this.snapToInitOn = snapToInitOn;
		this.stateToExplore = stateToExplore;
		this.explorationDuration = explorationDuration;
		this.event = Optional.ofNullable(event);
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
	public Optional<DESEvent> getEvent() {
		return this.event;
	}

	@Override
	public String toString() {
		return "SimulationInitConfiguration [snapToInitOn=" + snapToInitOn + ", stateToExplore=" + stateToExplore
				+ ", explorationDuration=" + explorationDuration + ", event=" + event + "]";
	}

}
