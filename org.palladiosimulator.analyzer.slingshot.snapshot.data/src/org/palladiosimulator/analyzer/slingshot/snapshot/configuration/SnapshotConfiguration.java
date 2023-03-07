package org.palladiosimulator.analyzer.slingshot.snapshot.configuration;

/**
 *
 * i'm probably putting this at the wrong places, put i need to place it somewhere.
 *
 * this configuration should contain simulation configuration regarding the snapshot / preinitialisation, such as
 * whether to start normal or with init events, when to take a snapshot, etc.
 *
 * recording snapshot :
 * - record at all (yes / no)
 * - point in time (single, interval)
 * - boolean condition (e.g. after reconfiguration triggered)
 *
 * starting from snapshot :
 * - yes / no
 * - which snapshot?
 *
 * @author stiesssh
 *
 */
public final class SnapshotConfiguration {

	private double interval;


	private final double significance;

	private final boolean startFromSnapshot;

	public SnapshotConfiguration(final double interval, final boolean startFromSnapshot, final double significance) {
		this.interval = interval;
		this.startFromSnapshot = startFromSnapshot;
		this.significance = significance;
	}

	public double getSignificance() {
		return this.significance;
	}

	public double getSnapinterval() {
		return this.interval;
	}

	public void resetInterval() {
		this.interval = 5.0;
	}

	public boolean isStartFromSnapshot() {
		return startFromSnapshot;
	}
}
