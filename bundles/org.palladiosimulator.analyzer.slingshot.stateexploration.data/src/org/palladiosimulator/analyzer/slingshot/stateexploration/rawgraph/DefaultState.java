package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;

/**
 * State in the raw state graph.
 *
 * Big State, with all the data i can get my hands on. to be condensed to the
 * necessary amount of data later on.
 *
 *
 * @author stiesssh
 *
 */
public class DefaultState implements RawModelState {

	private final DefaultGraph graph;

	/* known at start */
	private final double startTime;
	private final ArchitectureConfiguration archConfig;

	/* known at the end */
	private double duration;
	private Snapshot snapshot;
	private final Set<ReasonToLeave> reasonsToLeave;
	private boolean decreaseInterval = false;

	/* known after configuration of the simulation run */
	private ExperimentSetting experimentSetting;

	private double utility = 0;

	private final Collection<SPDAdjustorStateValues> adjustorStateValues;

	protected DefaultState(final double pointInTime, final ArchitectureConfiguration archConfig,
			final DefaultGraph graph) {
		this.graph = graph;
		this.startTime = pointInTime;
		this.archConfig = archConfig;
		this.reasonsToLeave = new HashSet<>();

		this.adjustorStateValues = new HashSet<>();
	}

	public ExperimentSetting getExperimentSetting() {
		return this.experimentSetting;
	}

	public void setExperimentSetting(final ExperimentSetting experimentSetting) {
		this.experimentSetting = experimentSetting;
	}

	public Snapshot getSnapshot() {
		return snapshot;
	}

	public void setSnapshot(final Snapshot snapshot) {
		this.snapshot = snapshot;
	}

	public boolean isDecreaseInterval() {
		return decreaseInterval;
	}

	public void setDecreaseInterval(final boolean decreaseInterval) {
		this.decreaseInterval = decreaseInterval;
	}

	public void addReasonToLeave(final ReasonToLeave reasonToLeave) {
		this.reasonsToLeave.add(reasonToLeave);
	}

	public void setDuration(final double duration) {
		this.duration = duration;
	}

	public double getUtility() {
		return utility;
	}

	public void setUtility(final double utility) {
		this.utility = utility;
	}

	public void addAdjustorStateValues(final Collection<SPDAdjustorStateValues> adjustorStateValues) {
		this.adjustorStateValues.addAll(adjustorStateValues);
	}

	public Set<SPDAdjustorStateValues> getAdjustorStateValues() {
		return Set.copyOf(this.adjustorStateValues);
	}

	/* to match the interface */

	@Override
	public ArchitectureConfiguration getArchitecureConfiguration() {
		return this.archConfig;
	}

	@Override
	public ExperimentSetting getMeasurements() {
		return this.getExperimentSetting();
	}


	@Override
	public Collection<ReasonToLeave> getReasonsToLeave() {
		return reasonsToLeave;
	}

	@Override
	public double getStartTime() {
		return this.startTime;
	}

	@Override
	public double getEndTime() {
		return this.startTime + this.duration;
	}

	@Override
	public double getDuration() {
		return this.duration;
	}

//	@Override
//	public String toString() {
//		return "DefaultState [archConfig=" + archConfig.getSegment() + ", reasonToLeave=" + reasonsToLeave + "]";
//	}
	
	@Override
	public String toString() {
		return "[" + archConfig.getSegment() + " (" + reasonsToLeave + ")]";
	}

	@Override
	public Optional<RawTransition> getIncomingTransition() {
		if (this.graph.getRoot().equals(this)) {
			return Optional.empty();
		}

		assert this.graph.incomingEdgesOf(this).size() == 1 : String.format("Illegal number of incoming edges for state %s.", this.toString());

		final Optional<RawTransition> transition = this.graph.incomingEdgesOf(this).stream().findFirst();

		if (transition.isEmpty()) {
			throw new IllegalStateException(String.format("No incoming edges for non-root state %s.", this.toString()));
		}

		return transition;
	}

	@Override
	public Set<RawTransition> getOutgoingTransitions() {
		return this.graph.outgoingEdgesOf(this);
	}

	/**
	 *
	 * @return distance between this state and root.
	 */
	public int lenghtOfHistory() {
		return DefaultGraph.distance(this, this.graph.getRoot());
	}
}
