package org.palladiosimulator.analyzer.slingshot.stateexploration.graph;

import java.util.Iterator;
import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 * Shared parent interface for all transitions, such that we are able to compare
 * {@link PlannedTransition}s from the fringe and actual {@link ExploredState}s
 * from the graph.
 * 
 * @author Sophie Stieß
 *
 */
public interface Transition {
	public Optional<Change> getChange();

	public ExploredState getSource();

	/**
	 * 
	 * {@link PlannedTransition}s and {@link RawTransition}s are the same, if they
	 * start at the same node and apply the same policies in the same order.
	 * Policies are compared by ID, i.e. they might be from different copies of the
	 * model.
	 * 
	 * @note i avoided overriding equals on purpose, because it's difficult to get
	 *       it right.
	 * 
	 * @param thisChange
	 * @param otherChange
	 * @return true, if the ToDoChanges are the same, false otherwise
	 */
	public default boolean isSame(final Transition otherChange) {
		if (!this.getSource().equals(otherChange.getSource())) {
			return false;
		}

		if (this.getChange().isPresent() && this.getChange().get() instanceof final Reconfiguration thisReconf
				&& otherChange.getChange().isPresent()
				&& otherChange.getChange().get() instanceof final Reconfiguration otherReconf) {

			final Iterator<ScalingPolicy> thisPolicies = thisReconf.getAppliedPolicies().iterator();
			final Iterator<ScalingPolicy> otherPolicies = otherReconf.getAppliedPolicies().iterator();

			while (thisPolicies.hasNext() && otherPolicies.hasNext()) {
				if (!thisPolicies.next().getId().equals(otherPolicies.next().getId())) {
					return false;
				}
			}
			return thisPolicies.hasNext() == otherPolicies.hasNext();
		} else {
			return this.getChange().isEmpty() == otherChange.getChange().isEmpty();
		}
	}
}
