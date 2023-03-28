package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.spd.ScalingPolicy;

public class SimulationInitConfiguration {

	private final Snapshot snapToInitOn;
	private final DefaultState stateToExplore;
	private final double explorationDuration;

	private final Optional<ScalingPolicy> policy;
	private final Optional<DESEvent> event;

	public SimulationInitConfiguration(final Snapshot snapToInitOn, final DefaultState stateToExplore, final double explorationDuration, final ScalingPolicy policy, final DESEvent event) {
		super();
		this.snapToInitOn = snapToInitOn;
		this.stateToExplore = stateToExplore;
		this.explorationDuration = explorationDuration;
		this.policy = Optional.ofNullable(policy);
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

	public Optional<ScalingPolicy> getPolicy() {
		return this.policy;
	}
	public Optional<DESEvent> getEvent() {
		return this.event;
	}
}
