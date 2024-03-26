package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.providers;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

/**
 * A provider for the {@link SimuComConfig} object that holds
 * all the information about the simulation.
 *
 */
@Singleton
public class DefaultStateProvider implements Provider<DefaultState> {

	private DefaultState config;

	public void set(final DefaultState config) {
		this.config = config;
	}

	@Override
	public DefaultState get() {
		return config;
	}

}
