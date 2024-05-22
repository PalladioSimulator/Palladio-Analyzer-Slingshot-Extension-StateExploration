package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;

/**
 *
 * Event to trigger the {@link GraphExplorer} to explore new states.
 *
 * @author Sarah Stieß
 *
 */
public class TriggerExplorationEvent extends AbstractExplorationControllerEvent {

	private final int iterations;

	/**
	 *
	 * @param iterations non-negative number of new state to explore.
	 */
	public TriggerExplorationEvent(final int iterations) {
		super();
		if (0 > iterations) {
			throw new IllegalArgumentException(
					String.format("Number of iterations must not be negative, but is %d.", iterations));
		}
		this.iterations = iterations;
	}

	public int getIterations() {
		return this.iterations;
	}

}
