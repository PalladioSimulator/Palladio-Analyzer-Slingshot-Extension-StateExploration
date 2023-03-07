package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;

public class SimulationInitConfiguration {

	private final Snapshot snapToInitOn;
	private final DefaultState stateToExplore;
	private final double explorationDuration;

	public SimulationInitConfiguration(final Snapshot snapToInitOn, final DefaultState stateToExplore, final double explorationDuration) {
		super();
		this.snapToInitOn = snapToInitOn;
		this.stateToExplore = stateToExplore;
		this.explorationDuration = explorationDuration;
	}
	public Snapshot getSnapToInitOn() {
		return snapToInitOn;
	}
	public DefaultState getStateToExplore() {
		return stateToExplore;
	}
	public double getExplorationDuration() {
		return explorationDuration;
	}




}
