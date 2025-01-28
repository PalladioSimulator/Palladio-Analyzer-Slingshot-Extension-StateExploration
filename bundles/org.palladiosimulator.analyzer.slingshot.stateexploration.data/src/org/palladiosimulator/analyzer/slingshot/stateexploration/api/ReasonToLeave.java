package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

/**
 * Describes the possible reasons for leaving a RawModelState, i.e., transitioning to the next.
 *
 * Attached to the state, because we the reason for leaving only depends on the
 * 
 * aborted : simulation of the state stopped right after starting it, usualy because the applied reconfiguration did not alter the system. 
 *
 * @author Sophie Stieß.
 *
 */
public enum ReasonToLeave {
	interval, reactiveReconfiguration, closenessToSLO, aborted;
}
