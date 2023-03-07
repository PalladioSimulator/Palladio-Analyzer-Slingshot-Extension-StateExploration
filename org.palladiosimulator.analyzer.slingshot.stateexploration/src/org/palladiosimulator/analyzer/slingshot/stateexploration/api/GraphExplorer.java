package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

public interface GraphExplorer {

	/**
	 * Start an exploration.
	 *
	 * TODO (1) : for now, we assume, that the planner requests a exploration
	 * (blocking), waits until the exploration is done and receives the
	 * RawStateGraph as result. For the future, it's more desirable, to have
	 * Planning and Exploration run independently, and the exploration notifies the
	 * Planning about updates in the RawStateGraph.
	 *
	 * TODO (2) : at some point, we gotta feed some configurations into the
	 * explorer, e.g. the horizon.
	 *
	 * @return a graph of explored states.
	 */
	public RawStateGraph start();
}
