package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.List;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.StateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ExploredState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ExploredTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.PlannedTransition;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 * TODO
 *
 * this is where i want to decide whether i will explore a given possible
 * future, or not. I have not yet any clue, how to decide o_O
 *
 * @author Sarah Stieß
 *
 */
public class CutOffConcerns {
	private static final Logger LOGGER = Logger.getLogger(CutOffConcerns.class.getName());
	
	private final StateGraph graph;

	private static final double BACKTRACK_FACTOR = 4;
	private final double backtrackDistance; 

	public CutOffConcerns(final StateGraph graph, final double minStateDuration) {
		super();
		this.graph = graph;
		this.backtrackDistance = BACKTRACK_FACTOR * minStateDuration;
	}

	public boolean shouldExplore(final PlannedTransition future) {
		LOGGER.debug(String.format("Evaluation future %s.", future.toString()));
		
		if ((graph.getFurthestState().getStartTime() - future.getStart().getStartTime()) > backtrackDistance) {
			return false;
		}
		
		return !matchesPattern(future);
	}

	/**
	 * Patter is "leav on rea" (prev) -> NOOP -> "leav on rea" (current) ([-> NOOP
	 * (ToDoChange])
	 * 
	 * Beware, this kinda interacts with the other cutoffs. As example: if we do not
	 * drop the scale ins on min config + abort effectless simulations, we stop the
	 * simulation after about 6 states.
	 *
	 * @param current
	 * @return
	 */
	private boolean matchesPattern(final PlannedTransition change) {

		final ExploredState current = change.getStart();

		if (current.getIncomingTransition().isEmpty()) { // root?
			return false;
		}

		final ExploredState prev = current.getIncomingTransition().get().getSource();

		return sameChange(current, prev) && bothNOOP(change, current.getIncomingTransition().get());
	}

	/**
	 *
	 * @param current
	 * @param prev
	 * @return
	 */
	private static boolean bothNOOP(final PlannedTransition current, final ExploredTransition prev) {
		return current.getChange().isEmpty() && prev.getChange().isEmpty();
	}
	
	private static boolean prevNOOP(final ExploredTransition prev) {
		return prev.getChange().isEmpty();
	}

	/**
	 *
	 * @param current
	 * @param prev
	 * @return
	 */
	private static boolean sameChange(final ExploredState current, final ExploredState prev) {
		if (current.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()
				|| prev.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()) {
			return false;
		}
		final List<ScalingPolicy> policyCurrent = current.getSnapshot().getModelAdjustmentRequestedEvent().stream()
				.map(e -> e.getScalingPolicy()).toList();
		final List<ScalingPolicy> policyPrev = prev.getSnapshot().getModelAdjustmentRequestedEvent().stream()
				.map(e -> e.getScalingPolicy()).toList();

		if (policyCurrent.size() != policyPrev.size()) {
			return false;
		}
		for (int i = 0; i < policyCurrent.size(); i++) {
			if (!policyCurrent.get(i).getId().equals(policyPrev.get(i).getId())) {
				return false;
			}
		}
		return true;
	}
}
