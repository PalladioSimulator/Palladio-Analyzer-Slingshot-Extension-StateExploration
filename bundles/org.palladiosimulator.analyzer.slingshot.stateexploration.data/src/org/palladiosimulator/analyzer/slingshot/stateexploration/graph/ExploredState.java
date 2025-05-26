package org.palladiosimulator.analyzer.slingshot.stateexploration.graph;

import java.util.Set;
import java.util.UUID;

import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;

/**
 * State for single state exploration.
 *
 * Big State, with all the data i can get my hands on. to be condensed to the
 * necessary amount of data later on.
 *
 *
 * @author Sophie Stieß
 *
 */
public class ExploredState {


	/* Additional fields*/
	private final String parentId;
	private final String id; 
	
	/* known at start */
	private final double startTime;

	/* known at the end */
	private final double duration;
	private final Snapshot snapshot;
	private final Set<ReasonToLeave> reasonsToLeave;

	/* known after configuration of the simulation run */
	private final ExperimentSetting experimentSetting;

	/**
	 * 
	 * @param pointInTime
	 * @param settings
	 * @param snapshot
	 * @param duration
	 * @param reasonsToLeave
	 * @param parentId
	 */
	protected ExploredState(final double pointInTime, final ExperimentSetting settings, final Snapshot snapshot, final double duration, final Set<ReasonToLeave> reasonsToLeave, final String parentId) {
		this.parentId = parentId;
		this.startTime = pointInTime;
		this.reasonsToLeave = reasonsToLeave;
		
		this.experimentSetting = settings;
		this.snapshot = snapshot;
		this.duration = duration;
		
		this.id = UUID.randomUUID().toString();
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

	public String getParentId() {
		return parentId;
	}

	public ExperimentSetting getExperimentSetting() {
		return this.experimentSetting;
	}

	public Set<ReasonToLeave> getReasonsToLeave() {
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
		return "[" + this.id + " (" + reasonsToLeave + ")]";
	}

	public String getId() {
		return this.id;
	}
}
