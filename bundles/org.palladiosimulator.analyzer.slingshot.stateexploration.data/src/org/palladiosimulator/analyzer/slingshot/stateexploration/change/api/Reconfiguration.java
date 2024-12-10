package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.common.base.Preconditions;

public abstract class Reconfiguration implements Change {

	private final List<ModelAdjustmentRequested> events;

	public Reconfiguration(final List<ModelAdjustmentRequested> events) {
		super();
		Preconditions.checkArgument(!events.isEmpty());
		Preconditions.checkNotNull(events);
		this.events = events;
	}

	public List<ModelAdjustmentRequested> getReactiveReconfigurationEvents() {
		return List.copyOf(events);
	}

	public Set<ScalingPolicy> getAppliedPolicies() {
		return events.stream().map(e -> e.getScalingPolicy()).collect(Collectors.toSet());
	}

	@Override
	public String toString() {
		return "Reconfiguration [events=" + events.stream()
				.map(e -> e.getScalingPolicy().getEntityName() + "[" + e.getScalingPolicy().getId() + "]")
				.reduce("", (a, b) -> a + ", " + b) + "]";
	}
}
