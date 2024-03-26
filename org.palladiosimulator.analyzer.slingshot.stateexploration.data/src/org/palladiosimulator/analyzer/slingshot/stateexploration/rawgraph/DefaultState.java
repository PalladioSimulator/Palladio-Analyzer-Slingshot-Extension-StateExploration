package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.HashSet;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.spd.ScalingPolicy;

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

	/* known at start */
	private final double startTime;
	private final ArchitectureConfiguration archConfig;

	/* known at the end */
	private double duration;
	/**
	 * at first misused to get the snapshot to init on into the simulation. Later on
	 * set to the actual snapshot of the events at the end of this state.
	 */
	private Snapshot snapshot;
	private ReasonToLeave reasonToLeave;
	private boolean decreaseInterval = false;

	/* known after configuration of the simulation run */
	private ExperimentSetting experimentSetting;

	/* changes upon creation of successive states */
	private final Set<RawTransition> outTransitions;

	public DefaultState(final double pointInTime, final ArchitectureConfiguration archConfig) {
		this.startTime = pointInTime;
		this.archConfig = archConfig;
		this.outTransitions = new HashSet<RawTransition>();
		this.reasonToLeave = ReasonToLeave.interval;
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

	public void addOutTransition(final RawTransition transition) {
		this.outTransitions.add(transition);
	}

	public void setReasonToLeave(final ReasonToLeave reasonToLeave) {
		this.reasonToLeave = reasonToLeave;
	}

	public void setDuration(final double duration) {
		this.duration = duration;
	}

	public boolean hasOutTransitionFor(final ScalingPolicy matchee) {
		return this.outTransitions.stream()
				.filter(t -> t.getChange().isPresent())
				.map(t -> t.getChange().get())
				.filter(c -> c instanceof final Reconfiguration r)
				.map(c -> ((Reconfiguration) c).getAppliedPolicy())
				.filter(policy -> policy.getId().equals(matchee.getId()))
				.findAny()
				.isPresent();
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
	public Set<RawTransition> getOutTransitions() {
		return outTransitions;
	}

	@Override
	public ReasonToLeave getReasonToLeave() {
		return reasonToLeave;
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

	@Override
	public String toString() {
		return "DefaultState [archConfig=" + archConfig.getSegment() + ", reasonToLeave=" + reasonToLeave + "]";
	}

}
