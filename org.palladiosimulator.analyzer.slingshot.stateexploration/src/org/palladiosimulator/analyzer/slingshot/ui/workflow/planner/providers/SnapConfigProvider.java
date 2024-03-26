package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.providers;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

/**
 * A provider for the {@link SimuComConfig} object that holds
 * all the information about the simulation.
 *
 */
@Singleton
public class SnapConfigProvider implements Provider<SnapshotConfiguration> {

	private SnapshotConfiguration config;

	public void set(final SnapshotConfiguration config) {
		this.config = config;
	}

	@Override
	public SnapshotConfiguration get() {
		return config;
	}

}
