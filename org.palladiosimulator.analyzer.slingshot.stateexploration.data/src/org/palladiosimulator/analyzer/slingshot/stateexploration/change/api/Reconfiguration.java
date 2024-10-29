package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

import java.util.Set;
import java.util.stream.Collectors;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.common.base.Preconditions;

public abstract class Reconfiguration implements Change {

	private final Set<ModelAdjustmentRequested> event;

	public Reconfiguration(final ModelAdjustmentRequested event) {
		super();
		this.event = Set.of(Preconditions.checkNotNull(event));
	}

	public Set<ModelAdjustmentRequested> getReactiveReconfigurationEvent() {
		return event;
	}

	public Set<ScalingPolicy> getAppliedPolicy() {
		return event.stream().map(e -> e.getScalingPolicy()).collect(Collectors.toSet());
	}
}
