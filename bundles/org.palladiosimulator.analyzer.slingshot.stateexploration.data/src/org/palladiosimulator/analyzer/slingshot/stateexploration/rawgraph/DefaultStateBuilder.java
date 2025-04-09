package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;

import com.google.common.base.Preconditions;

/**
 * Builder Class for {@link DefaultState}.
 * 
 * Cannot be used to build the root node.
 *
 * Some values from the builder are required for initialising a simulation run,
 * while others are required for post processing. Those are wrapped into record
 * types, as i do not want arbitrary getters in this class.
 *
 * @author Sophie Stieß
 *
 */
public class DefaultStateBuilder {

	/* known at start */
	private final DefaultGraph graph;
	private final DefaultState predecessor;
	private final Optional<Change> change;
	private final double startTime;
	private final ArchitectureConfiguration archConfig;

	/* must be filled at the end of a simulation run */
	private final Set<ReasonToLeave> reasonsToLeave = new HashSet<>();

	/* may be filled at the end of a simulation run */
	private final Collection<SPDAdjustorStateValues> adjustorStateValues = new HashSet<>();

	/* must be set at the end of a simulation run */
	private double duration = -1;
	private Snapshot snapshot = null;

	/* must be set after configuration of the simulation run */
	private ExperimentSetting experimentSetting = null;

	public DefaultStateBuilder(final DefaultGraph graph, final PlannedTransition plannedTransition) {
		this.graph = graph;
		this.predecessor = plannedTransition.getStart();
		this.change = plannedTransition.getChange();
		this.startTime = plannedTransition.getSource().getEndTime();
		this.archConfig = plannedTransition.getSource().getArchitecureConfiguration().copy();

		this.info = new StartupInfo(this.predecessor, this.archConfig, this.startTime);
		this.ppInfo = new PostProcessInfo(this.predecessor, this.change);
	}

	public void setExperimentSetting(final ExperimentSetting experimentSetting) {
		this.experimentSetting = experimentSetting;
	}

	public void setSnapshot(final Snapshot snapshot) {
		this.snapshot = snapshot;
	}

	public void setDuration(final double duration) {
		this.duration = duration;
	}

	public void addReasonToLeave(final ReasonToLeave reasonToLeave) {
		this.reasonsToLeave.add(reasonToLeave);
	}

	public void addAdjustorStateValues(final Collection<SPDAdjustorStateValues> adjustorStateValues) {
		this.adjustorStateValues.addAll(adjustorStateValues);
	}

	/**
	 * Information about the state to be build by this builder, that are required
	 * while preprocessing and initialising the simulation run.
	 */
	public record StartupInfo(DefaultState predecessor, ArchitectureConfiguration architecureConfiguration,
			double startTime) {
	}

	private final StartupInfo info;

	/**
	 * Information about the state to be build by this builder, that are required
	 * while postprocessing the simulation run.
	 */
	public record PostProcessInfo(DefaultState predecessor, Optional<Change> change) {
	}

	private final PostProcessInfo ppInfo;

	public PostProcessInfo getPPInfo() {
		return this.ppInfo;
	}

	public StartupInfo getStartupInformation() {
		return this.info;
	}

	/**
	 * Build a new {@link DefaultState} based on this builder.
	 * 
	 * Requires all attributes to be set. If some attributes are missing or were not
	 * updated as intended, building the state fails.
	 * 
	 * @return a new {@link DefaultState}
	 * @throws IllegalStateException if this operation is called while the builder
	 *                               is still incomplete.
	 */
	protected DefaultState build() {
		Preconditions.checkState(!reasonsToLeave.isEmpty(), "Cannot build state, reasons to leave were not yet added.");
		Preconditions.checkState(duration >= 0, "Cannot build state, duration was not yet set.");
		Preconditions.checkState(snapshot != null, "Cannot build state, because snapshot was not yet set.");
		Preconditions.checkState(experimentSetting != null,
				"Cannot build state, because experiment settings were not yet set.");

		return new DefaultState(startTime, archConfig, graph, experimentSetting, snapshot, duration,
				adjustorStateValues, reasonsToLeave);
	}
}
