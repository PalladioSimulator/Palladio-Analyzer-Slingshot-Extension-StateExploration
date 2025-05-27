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
public class PlannedTransition implements Transition {
	private final ExploredState start;
	private final Optional<Change> change;

	public PlannedTransition(final Optional<Change> change, final ExploredState start) {
		super();
		this.start = start;
		this.change = change;
	}

	public ExploredState getStart() {
		return start;
	}
	
	@Override
	public ExploredState getSource(){
		return this.getStart();
	}

	@Override
	public Optional<Change> getChange() {
		return change;
	}

	@Override
	public String toString() {
		if (change.isEmpty()) {
			return "NOP for " + start.getId();
		} else {
			return String.format("%s for %s", change.get().toString(), start.getId());
		}
	}
	


}
