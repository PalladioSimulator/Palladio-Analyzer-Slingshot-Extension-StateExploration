package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.ModelElementDifference;
import org.palladiosimulator.pcm.core.entity.Entity;

public class DefaultTransition implements RawTransition {
	private final Optional<Change> change;
	private final Set<ModelElementDifference<Entity>> modelDiff;
	private final DefaultState start;
	private final DefaultState end;


	public DefaultTransition(final Optional<Change> change, final DefaultState start, final DefaultState end) {
		super();
		this.change = change;
		this.start = start;
		this.end = end;
		this.modelDiff = new HashSet<>();
	}

	public void addDifferences(final Set<ModelElementDifference<Entity>> diffs) {
		this.modelDiff.addAll(diffs);
	}

	@Override
	public RawModelState getSource() {
		return this.start;
	}

	@Override
	public RawModelState getTarget() {
		return this.end;
	}

	@Override
	public Optional<Change> getChange() {
		return this.change;
	}

	@Override
	public Set<ModelElementDifference<Entity>> getModelDifferences() {
		return this.modelDiff;
	}
}
