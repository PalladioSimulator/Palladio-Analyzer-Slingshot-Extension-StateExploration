package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import java.util.Set;

/**
 *
 * Graph of RawModelStates, as produced during Exploration.
 *
 *
 * @author Sophie Stieß
 *
 */
public interface RawStateGraph {

	/**
	 *
	 * @return
	 */
	public RawModelState getRoot();

	public Set<RawModelState> getStates();

	public Set<RawTransition> getTransitions();

}
