package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
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
	private final DefaultState start;
	private final Optional<Change> change;

	public PlannedTransition(final Optional<Change> change, final DefaultState start) {
		super();
		this.start = start;
		this.change = change;
	}

	public DefaultState getStart() {
		return start;
	}
	
	@Override
	public RawModelState getSource(){
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
