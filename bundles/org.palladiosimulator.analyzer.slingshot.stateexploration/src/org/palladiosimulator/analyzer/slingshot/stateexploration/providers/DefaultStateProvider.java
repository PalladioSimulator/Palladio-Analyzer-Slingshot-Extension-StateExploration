package org.palladiosimulator.analyzer.slingshot.stateexploration.providers;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ExploredStateBuilder;


/**
 * Provides the {@link RawModelStateBuilder} that represents the next simulation run.
 *
 * @author Sarah Stieß
 *
 */
@Singleton
public class DefaultStateProvider implements Provider<ExploredStateBuilder> {

	private ExploredStateBuilder state;

	public void set(final ExploredStateBuilder state) {
		this.state = state;
	}

	@Override
	public ExploredStateBuilder get() {
		return state;
	}

}
