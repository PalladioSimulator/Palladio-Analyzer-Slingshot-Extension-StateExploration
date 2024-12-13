package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;

/**
 * TODO
 *
 * @author Sarah Stieß
 *
 */
public class PruneFringeByTime extends EventMessage<Double> implements ExplorationControllerEvent {

	public static final String MESSAGE_MAPPING_IDENTIFIER = PruneFringeByTime.class.getSimpleName();

	public PruneFringeByTime(final double currentTime) {
		super(MESSAGE_MAPPING_IDENTIFIER, currentTime);
		if (currentTime < 0) {
			throw new IllegalArgumentException(
					String.format("Argument must be greater equal 0, but is %d.", currentTime));
		}
	}

	public double getCurrentTime() {
		return this.getPayload();
	}
}
