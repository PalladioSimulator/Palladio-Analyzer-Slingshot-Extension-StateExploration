package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 *
 * Event to trigger the {@link GraphExplorer} to explore new states.
 *
 * @author Sarah Stieß
 *
 */
public class TriggerExplorationEvent extends EventMessage<Integer> implements ExplorationControllerEvent {

	public static final String MESSAGE_MAPPING_IDENTIFIER = TriggerExplorationEvent.class.getSimpleName();

	/**
	 *
	 * @param iterations non-negative number of new state to explore.
	 */
	public TriggerExplorationEvent(final int iterations) {
		super(MESSAGE_MAPPING_IDENTIFIER, iterations);

		if (iterations < 0) {
			throw new IllegalArgumentException(
					String.format("Number of iterations must not be negative, but is %d.", iterations));
		}
	}

	public int getIterations() {
		return this.getPayload();
	}

}
