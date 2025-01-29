package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import java.util.Collection;

import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.PlannedTransition;

/**
 *
 * Explores the state graph.
 *
 * @author Sarah Stieß
 *
 */
public interface GraphExplorer {

	/**
	 * Explore the next state.
	 */
	public void exploreNextState();


	/**
	 *
	 * @return true, iff more unexplored changes are available.
	 */
	public boolean hasUnexploredChanges();

	/**
	 *
	 * @return the state graph
	 */
	public RawStateGraph getGraph();

	/**
	 *
	 * Focus exploration on the given model states by removing all
	 * {@link PlannedTransition}s that are unrelated to the given states from the fringe.
	 *
	 * @param focusStates states to focus on.
	 */
	public void focus(Collection<RawModelState> focusStates);

	/**
	 *
	 * Add the given model states back into focus by (re-)adding {@link PlannedTransition}s
	 * for the given states to the fringe.
	 *
	 * Only adds {@link PlannedTransition}s for unexplored futures, i.e. for already fully
	 * explored state, no new changes are added.
	 *
	 * @param focusStates states to refocus on.
	 */
	public void refocus(Collection<RawModelState> focusStates);

	/**
	 *
	 * Remove all elements from the fringe that explore futures that start in the
	 * past.
	 *
	 * @param time current time in the real system
	 */
	public void pruneByTime(double time);
}
