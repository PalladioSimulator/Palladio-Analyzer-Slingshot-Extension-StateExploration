package org.palladiosimulator.analyzer.slingshot.stateexploration.providers;

import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;

/**
 * Provides the {@link SnapshotConfiguration} for starting a simulation run that
 * can be snapshotted.
 *
 * @author Sarah Stieß
 *
 */
@Singleton
public class SnapshotConfigurationProvider implements Provider<SnapshotConfiguration> {

	private SnapshotConfiguration config;

	public void set(final SnapshotConfiguration config) {
		this.config = config;
	}

	@Override
	public SnapshotConfiguration get() {
		return config;
	}

}
