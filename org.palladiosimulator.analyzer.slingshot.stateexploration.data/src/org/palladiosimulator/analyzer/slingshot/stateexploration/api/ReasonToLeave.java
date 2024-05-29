package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

/**
 * Describes the possible reasons for leaving a RawModelState, i.e., transitioning to the next.
 *
 * Attached to the state, because we the reason for leaving only depends on the
 *
 * @author stiesssh
 *
 */
public enum ReasonToLeave {
	interval, reactiveReconfiguration, closenessToSLO;
}
