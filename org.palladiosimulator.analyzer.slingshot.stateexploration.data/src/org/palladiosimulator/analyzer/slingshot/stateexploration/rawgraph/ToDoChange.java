package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;

public class ToDoChange {
	private final DefaultState start;
	private final Optional<Change> change;

	public ToDoChange(final Optional<Change> change, final DefaultState start) {
		super();
		this.start = start;
		this.change = change;
	}

	public DefaultState getStart() {
		return start;
	}
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
