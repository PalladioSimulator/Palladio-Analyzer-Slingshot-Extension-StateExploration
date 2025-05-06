package org.palladiosimulator.analyzer.slingshot.stateexploration.providers;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultStateBuilder;


/**
 * Provides the {@link RawModelStateBuilder} that represents the next simulation run.
 *
 * @author Sarah Stieß
 *
 */
@Singleton
public class DefaultStateProvider implements Provider<DefaultStateBuilder> {

	private DefaultStateBuilder state;

	public void set(final DefaultStateBuilder state) {
		this.state = state;
	}

	@Override
	public DefaultStateBuilder get() {
		return state;
	}

}
