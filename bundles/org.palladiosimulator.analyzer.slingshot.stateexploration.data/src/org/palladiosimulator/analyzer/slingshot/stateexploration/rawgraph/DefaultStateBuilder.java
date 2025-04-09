package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemorySnapshot;
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
public class DefaultStateBuilder {

	private final DefaultGraph graph;

	private final PlannedTransition plannedTransition;
	
	/* known at start */
	private final double startTime;
	private final ArchitectureConfiguration archConfig;

	/* known at the end */
	private double duration;
	private Snapshot snapshot;
	private final Set<ReasonToLeave> reasonsToLeave;

	/* known after configuration of the simulation run */
	private ExperimentSetting experimentSetting;

	private double utility = 0;

	private final Collection<SPDAdjustorStateValues> adjustorStateValues;

	public DefaultStateBuilder(final DefaultGraph graph, final PlannedTransition plannedTransition) {
		this.graph = graph;
		this.startTime = plannedTransition.getSource().getEndTime();
		this.archConfig = plannedTransition.getSource().getArchitecureConfiguration().copy();
		this.reasonsToLeave = new HashSet<>();

		this.adjustorStateValues = new HashSet<>();
		
		this.plannedTransition = plannedTransition;
	}
	
	public static DefaultStateBuilder getRootNodeBuilder(final DefaultGraph graph, final ArchitectureConfiguration archConfig) {
		return new DefaultStateBuilder(graph, archConfig);
	}
	
	private DefaultStateBuilder(final DefaultGraph graph, final ArchitectureConfiguration archConfig) {
		this.graph = graph;
		this.startTime = 0;
		this.archConfig = archConfig;
		this.reasonsToLeave = new HashSet<>();

		this.adjustorStateValues = new HashSet<>();
		this.snapshot = new InMemorySnapshot(Set.of());
		this.plannedTransition = null;
	}

	public PlannedTransition getPlannedTransition() {
		return this.plannedTransition;
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
	
	public String getId() {
		return getArchitecureConfiguration().getSegment();
	}

	public void setSnapshot(final Snapshot snapshot) {
		this.snapshot = snapshot;
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

	public ArchitectureConfiguration getArchitecureConfiguration() {
		return this.archConfig;
	}

	public ExperimentSetting getMeasurements() {
		return this.getExperimentSetting();
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

	/**
	 *
	 * @return distance between this state and root.
	 */
	//public int lenghtOfHistory() {
	//	return DefaultGraph.distance(this, this.graph.getRoot());
	//}
	
	protected DefaultState build() {
		return new DefaultState(startTime, archConfig, graph, experimentSetting, snapshot, duration, adjustorStateValues, reasonsToLeave);
	}
}
