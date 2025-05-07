package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
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
public class ExploredState {

	private final StateGraph graph;

	/* known at start */
	private final double startTime;
	private final ArchitectureConfiguration archConfig;

	/* known at the end */
	private final double duration;
	private final Snapshot snapshot;
	private final Set<ReasonToLeave> reasonsToLeave;

	/* known after configuration of the simulation run */
	private final ExperimentSetting experimentSetting;

	private double utility = 0;

	/**
	 * 
	 * @param pointInTime
	 * @param archConfig
	 * @param graph
	 * @param settings
	 * @param snapshot
	 * @param duration
	 * @param adjustorStateValues
	 * @param reasonsToLeave
	 */
	protected ExploredState(final double pointInTime, final ArchitectureConfiguration archConfig,
			final StateGraph graph, final ExperimentSetting settings, final Snapshot snapshot, final double duration, final Set<ReasonToLeave> reasonsToLeave) {
		this.graph = graph;
		this.startTime = pointInTime;
		this.archConfig = archConfig;
		this.reasonsToLeave = reasonsToLeave;
		
		this.experimentSetting = settings;
		this.snapshot = snapshot;

		this.duration = duration;
	}

	/**
	 * Get the snapshot of the simulator state at the end of this state.
	 * 
	 * Needed start a new simulation run that resumes the simulation at the end of this state.
	 * 
	 * @return snapshot of the Simulator state at the end of this state.
	 */
	public Snapshot getSnapshot() {
		return snapshot;
	}

	public double getUtility() {
		return utility;
	}

	public void setUtility(final double utility) {
		this.utility = utility;
	}

	/**
	 *
	 * @return distance between this state and root.
	 */
	public int lenghtOfHistory() {
		return StateGraph.distance(this, this.graph.getRoot());
	}

	public ArchitectureConfiguration getArchitecureConfiguration() {
		return this.archConfig;
	}

	public ExperimentSetting getExperimentSetting() {
		return this.experimentSetting;
	}

	public Collection<ReasonToLeave> getReasonsToLeave() {
		return reasonsToLeave;
	}

	public double getStartTime() {
		return this.startTime;
	}

	public double getEndTime() {
		return this.startTime + this.duration;
	}

	public double getDuration() {
		return this.duration;
	}
	
	@Override
	public String toString() {
		return "[" + archConfig.getSegment() + " (" + reasonsToLeave + ")]";
	}

	public Optional<ExploredTransition> getIncomingTransition() {
		if (this.graph.getRoot().equals(this)) {
			return Optional.empty();
		}

		assert this.graph.incomingEdgesOf(this).size() == 1 : String.format("Illegal number of incoming edges for state %s.", this.toString());

		final Optional<ExploredTransition> transition = this.graph.incomingEdgesOf(this).stream().findFirst();

		if (transition.isEmpty()) {
			throw new IllegalStateException(String.format("No incoming edges for non-root state %s.", this.toString()));
		}

		return transition;
	}

	public Set<ExploredTransition> getOutgoingTransitions() {
		return this.graph.outgoingEdgesOf(this);
	}
	
	public String getId() {
		return getArchitecureConfiguration().getSegment();
	}
}
