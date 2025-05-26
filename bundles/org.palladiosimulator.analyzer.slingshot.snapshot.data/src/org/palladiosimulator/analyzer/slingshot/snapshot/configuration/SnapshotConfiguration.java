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
 * @author Sophie Stieß
 *
 */
public final class SnapshotConfiguration {

	private final double minDuration;
	private final double sensitivity;
	private final boolean startFromSnapshot;

	public SnapshotConfiguration(final boolean startFromSnapshot, final double sensitivity, final double minDuration) {
		this.startFromSnapshot = startFromSnapshot;
		this.sensitivity = sensitivity;
		this.minDuration = minDuration;
	}

	public double getMinDuration() {
		return this.minDuration;
	}

	public double getSensitivity() {
		return this.sensitivity;
	}

	public boolean isStartFromSnapshot() {
		return startFromSnapshot;
	}
}
