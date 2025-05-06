package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

/**
 * Describes the possible reasons for terminating a simulation run, i.e
 * finalising one model state and transitioning to the next.
 *
 * Those reasons are not intended to be mutually exclusive. E.g., if the
 * simulation run stops after the predefined simulation time t_max, but a
 * reconfiguration is triggered at t_max as well, then the simulation run should
 * be tagged with both reasons.
 * 
 * @author Sophie Stieß
 *
 */
public enum ReasonToLeave {
	/** 
	 * the simulation run ended after the predefined simulation time. 
	 */
	interval,
	/** 
	 * the simulation run ended because a reactive reconfiguration occurred. 
	 */
	reactiveReconfiguration,
	/**
	 * the simulation run ended because some measurements were too close to an SLO.
	 */
	closenessToSLO,
	/**
	 * the simulation run ended because it is no worth to continue exploring this
	 * branch.
	 */
	aborted;
}
