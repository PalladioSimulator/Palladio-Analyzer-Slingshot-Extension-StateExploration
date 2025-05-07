package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.Transition;

/**
 * Transition between two RawModelStates.
 *
 * They are directed and carry information about the change that the got us from the transition's source state to it's target state.
 *
 * @author Sophie Stieß
 *
 */
public interface RawTransition extends Transition {

	@Override
	public RawModelState getSource();
	public RawModelState getTarget();

	/**
	 * Point in time of the transition.
	 *
	 * @return point in time in seconds
	 */
	public default double getPointInTime() {
		return this.getSource().getEndTime();
	}

	/**
	 * Change means change in the PCM instances.
	 *
	 * i.e. if there no explicit "change", its only changes in runtime measurement
	 *
	 * if there is an change, its either an EnvironmentChange, or an Reconfiguration.
	 * This is kinda selfexplanatory though, i guess?
	 *
	 * @return
	 */
	@Override
	public Optional<Change> getChange();

	/**
	 * Concatenate source and target id to transistion name.
	 *
	 * @return
	 */
	default String getName() {
		return String.format("%s -> %s", getSource().getId(),getTarget().getId());
	}
}
