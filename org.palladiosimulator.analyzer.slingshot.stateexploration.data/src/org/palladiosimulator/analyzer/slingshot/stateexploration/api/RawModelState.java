package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import java.util.Set;

import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;

/**
 *
 * State of the RawGraph, as recorded during Exploration.
 *
 * @author stiesssh
 *
 */
public interface RawModelState {

	/**
	 * Get everything PCM.
	 *
	 * @return
	 */
	public SetBasedArchitectureConfiguration getArchitecureConfiguration();

	/**
	 * Get Measurements as EDP2
	 *
	 * @return
	 */
	public ExperimentSetting getMeasurements();

	/**
	 * Get RawTransitions that start at the current state.
	 *
	 * @return
	 */
	public Set<RawTransition> getOutTransitions();

	/**
	 * Get the point in time the state started
	 *
	 * @return point in time in seconds
	 */
	public double getStartTime();

	/**
	 * Get the point in time the state ended
	 *
	 * @return point in time in seconds
	 */
	public double getEndTime();

	/**
	 * Get the duration the state lasted. Only applicable after the state is over.
	 *
	 * @return duration in seconds
	 */
	public double getDuration();

	/**
	 *
	 * For now, let's identify each state by the last segment of the path to its pcm
	 * instances. For the root state, returns the name of the project, as the root
	 * state's models are not in any sub folder.
	 *
	 * @return Id for the state
	 */
	public default String getId() {
		return getArchitecureConfiguration().getSegment();
	}

	/**
	 * The exploration's reason for deciding to leave the state.
	 *
	 * @return
	 */
	public ReasonToLeave getReasonToLeave();

	// getSLOFullfillment() ?
}
