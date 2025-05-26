package org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.data;

import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;

/**
 *
 * @author Sophie Stieß
 *
 */
public class InitState {

	private final String id;
	
	private final double pointInTime;
	private final Snapshot snapshot;
	
	/**
	 * 
	 * @param pointInTime
	 * @param settings
	 * @param snapshot
	 * @param duration
	 * @param reasonsToLeave
	 * @param parentId
	 */
	public InitState(final double pointInTime, final Snapshot snapshot, final String id) {
		this.id = id;
		this.pointInTime = pointInTime;
		this.snapshot = snapshot;
		
	}

	public Snapshot getSnapshot() {
		return snapshot;
	}

	public String getId() {
		return id;
	}

	public double getPointInTime() {
		return this.pointInTime;
	}
}
