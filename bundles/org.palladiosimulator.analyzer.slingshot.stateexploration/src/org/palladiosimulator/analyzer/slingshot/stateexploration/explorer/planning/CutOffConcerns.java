package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.List;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
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

	public boolean shouldExplore(final PlannedTransition future) {
		LOGGER.debug(String.format("Evaluation future %s.", future.toString()));
		LOGGER.debug(String.format("Future %s is rosy, will explore.", future.toString()));

		return !matchesPattern(future);
	}

	/**
	 * Patter is "leav on rea" (prev) -> NOOP -> "leav on rea" (current) -> NOOP
	 * (ToDoChange)
	 *
	 * @param current
	 * @return
	 */
	private boolean matchesPattern(final PlannedTransition change) {

		final DefaultState current = change.getStart();

		if (current.getIncomingTransition().isEmpty()) { // root?
			return false;
		}

		final DefaultState prev = (DefaultState) current.getIncomingTransition().get().getSource();

		return sameChange(current, prev) && bothNOOP(change, current.getIncomingTransition().get());
	}

	/**
	 *
	 * @param current
	 * @param prev
	 * @return
	 */
	private static boolean bothNOOP(final PlannedTransition current, final RawTransition prev) {
		return current.getChange().isEmpty() && prev.getChange().isEmpty();
	}

	/**
	 *
	 * @param current
	 * @param prev
	 * @return
	 */
	private static boolean sameChange(final DefaultState current, final DefaultState prev) {
		if (current.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()
				|| prev.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()) {
			return false;
		}
		final List<ScalingPolicy> policyCurrent = current.getSnapshot().getModelAdjustmentRequestedEvent().stream().map(e -> e.getScalingPolicy()).toList();
		final List<ScalingPolicy> policyPrev = prev.getSnapshot().getModelAdjustmentRequestedEvent().stream().map(e -> e.getScalingPolicy()).toList();
		
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
