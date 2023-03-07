package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import java.util.Optional;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.EnvironmentChange;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ModelElementDifference;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.pcm.core.entity.Entity;

/**
 * Transition between two RawModelStates.
 *
 * They are directed and carry information about the change that the got us from the transition's source state to it's target state.
 *
 * @author stiesssh
 *
 */
public interface RawTransition {

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
	public Optional<Change> getChange();

	/**
	 * actually, you can also calculate this from the source and target model instances.
	 * dont know whether it makes sense to provide this.
	 *
	 * @return
	 */
	public Set<ModelElementDifference<Entity>> getModelDifferences();


	/**
	 *
	 * @return
	 */
	default TransitionType getType() {
		if (this.getChange().isEmpty()) {
			return TransitionType.NOP;
		} else if (this.getChange().get() instanceof Reconfiguration) {
			return TransitionType.reconfiguration;
		} else if (this.getChange().get() instanceof EnvironmentChange) {
			return TransitionType.environmentChange;
		}
		return TransitionType.unknown;
	}

	/**
	 * Concatenate source and target id to transistion name.
	 *
	 * @return
	 */
	default String getName() {
		return String.format("%s -> %s", getSource().getId(),getTarget().getId());
	}
}
