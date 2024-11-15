package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

import java.util.Set;
import java.util.stream.Collectors;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.common.base.Preconditions;

public abstract class Reconfiguration implements Change {

	private final Set<ModelAdjustmentRequested> events;

	public Reconfiguration(final Set<ModelAdjustmentRequested> events) {
		super();
		Preconditions.checkArgument(!events.isEmpty());
		Preconditions.checkNotNull(events);
		this.events = events;
	}

	public Set<ModelAdjustmentRequested> getReactiveReconfigurationEvents() {
		return Set.copyOf(events);
	}

	public Set<ScalingPolicy> getAppliedPolicies() {
		return events.stream().map(e -> e.getScalingPolicy()).collect(Collectors.toSet());
	}
}
