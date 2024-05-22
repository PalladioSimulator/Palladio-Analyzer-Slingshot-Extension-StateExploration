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
	 *
	 * @return a graph of explored states.
	 */
	public void start();

	/**
	 * stop + Reset the Graphexplorer.
	 *
	 * Clear the explored states and the fringe.
	 */
	public void reset();

	/**
	 * reset + start;
	 */
	public void restart();

	/**
	 * in accurate name. rename.
	 *
	 * @return
	 */
	public boolean hasNext();

	/**
	 *
	 * @return
	 */
	public RawStateGraph getGraph();

	/**
	 *
	 * Focus exploration on {@code modelState}.
	 *
	 * Maybe also with the plan. But i'm not
	 *
	 * 5. [onEvent] Wenn (E) Plan vom Planer erhält: * exploration am Ende des plans
	 * weiter machen / fokusieren.
	 *
	 *
	 * @param modelState
	 */
	public void focus(RawModelState modelState);

}
