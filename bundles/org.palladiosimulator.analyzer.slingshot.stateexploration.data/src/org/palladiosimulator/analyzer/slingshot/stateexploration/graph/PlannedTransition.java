package org.palladiosimulator.analyzer.slingshot.stateexploration.graph;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;

/**
 * 
 * Transition to a state, that does not yet exist. 
 * 
 * {@link PlannedTransition} are for populating the fringe. 
 * 
 * 
 * @author Sophie Stieß
 *
 */
public class PlannedTransition {
	private final Optional<Change> change;

	public PlannedTransition(final Optional<Change> change) {
		super();
		this.change = change;
	}
	
	public Optional<Change> getChange() {
		return change;
	}
}
