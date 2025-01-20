package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Iterator;
import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.spd.ScalingPolicy;

public interface Transition {
	public Optional<Change> getChange();
	public RawModelState getSource();
	
	/**
	 * 
	 * {@link PlannedTransition}s and {@link RawTransition}s are the same, if they start at the same node and apply the same policies in the same order. 
	 * Policies are compared by ID, i.e. they might be from different copies of the model. 
	 * 
	 * @note i avoided overriding equals on purpose, because it's difficult to get it right. 
	 * 
	 * @param thisChange
	 * @param otherChange
	 * @return true, if the ToDoChanges are the same, false otherwise
	 */
	 public default boolean isSame(final Transition otherChange) {
		if (! this.getSource().equals(otherChange.getSource())) {
			return false;
		}
		
		if (this.getChange().isPresent() && this.getChange().get() instanceof Reconfiguration thisReconf
				&& otherChange.getChange().isPresent() && otherChange.getChange().get() instanceof Reconfiguration otherReconf ) {
			
			
			Iterator<ScalingPolicy> thisPolicies = thisReconf.getAppliedPolicies().iterator();
			Iterator<ScalingPolicy> otherPolicies = otherReconf.getAppliedPolicies().iterator();
			
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
