package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

/**
 * TODO
 *
 * @author Sarah Stieß
 *
 */
public class PruneFringeByTime extends AbstractExplorationControllerEvent {

	private final double currentTime;

	public PruneFringeByTime(final double currentTime) {
		super();
		this.currentTime = currentTime;
	}

	public double getCurrentTime() {
		return currentTime;
	}
}
