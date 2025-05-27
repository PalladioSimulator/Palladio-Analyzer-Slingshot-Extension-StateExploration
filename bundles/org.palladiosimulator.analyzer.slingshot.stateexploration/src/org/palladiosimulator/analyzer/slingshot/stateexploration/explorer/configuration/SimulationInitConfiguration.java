package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration;

import java.util.List;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateInitialized;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.ExploredStateBuilder;

/**
 * Configuration that holds all (most?) information for starting a new
 * simulation cycle with the exploration.
 *
 * @author Sarah Stieß
 *
 */
public class SimulationInitConfiguration {

	private final Snapshot snapToInitOn;
	private final ExploredStateBuilder stateToExplore;
	private final double explorationDuration;

	/** Events that are not part of the simulation, but only for initialising the SPD Interpreter. */
	private final Set<SPDAdjustorStateInitialized> initializationEvents;

	/** Adjustments. Beware, order must be preserved.*/
	private final List<ModelAdjustmentRequested> events;

	public SimulationInitConfiguration(final Snapshot snapToInitOn, final ExploredStateBuilder stateToExplore,
			final double explorationDuration, final List<ModelAdjustmentRequested> events,
			final Set<SPDAdjustorStateInitialized> initializationEvents) {
		super();
		this.snapToInitOn = snapToInitOn;
		this.stateToExplore = stateToExplore;
		this.explorationDuration = explorationDuration;
		this.events = events;
		this.initializationEvents = initializationEvents;
	}

	public Snapshot getSnapToInitOn() {
		return this.snapToInitOn;
	}

	public ExploredStateBuilder getStateToExplore() {
		return this.stateToExplore;
	}

	public double getExplorationDuration() {
		return this.explorationDuration;
	}

	public Set<SPDAdjustorStateInitialized> getStateInitializationEvents() {
		return this.initializationEvents;
	}

	/**
	 * Get ScalingEvent for reactive reconfiguration transition
	 *
	 * @return the event
	 */
	public List<ModelAdjustmentRequested> getAdjustmentEvents() {
		return this.events;
	}

	@Override
	public String toString() {
		return "SimulationInitConfiguration [stateToExplore=" + stateToExplore
				+ ", explorationDuration=" + explorationDuration + ", policies=[" + events.stream()
				.map(e -> e.getScalingPolicy().getEntityName() + "(" + e.getScalingPolicy().getId() + ")")
				.reduce("", (a, b) -> a + ", " + b)
				+ "] - "
				+ " application "
				+ "]";
	}
}
