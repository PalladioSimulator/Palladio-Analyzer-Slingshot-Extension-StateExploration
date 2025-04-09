package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import java.util.Set;

/**
 *
 * Represents the different state explored during a state space exploration.
 *
 * @author Sophie Stieß
 */
public interface RawStateGraph {

	/**
	 * Get root node of this graph.
	 * 
	 * @return root node of this graph.
	 */
	public RawModelState getRoot();

	/**
	 * Get all states of this graph.
	 * 
	 * @return all states of this graph.
	 */
	public Set<RawModelState> getStates();

	
	/**
	 * Get all transitions of this graph.
	 * 
	 * @return all transitions of this graph.
	 */
	public Set<RawTransition> getTransitions();

}
