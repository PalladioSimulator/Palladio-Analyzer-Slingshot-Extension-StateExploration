package org.palladiosimulator.analyzer.slingshot.stateexploration.providers;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;


/**
 * Provides the {@link RawModelState} that represents the next simulation run.
 *
 * @author Sarah Stieß
 *
 */
@Singleton
public class DefaultStateProvider implements Provider<DefaultState> {

	private DefaultState state;

	public void set(final DefaultState state) {
		this.state = state;
	}

	@Override
	public DefaultState get() {
		return state;
	}

}
