package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ToDoChange;
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

	public boolean shouldExplore(final ToDoChange future) {
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
	private boolean matchesPattern(final ToDoChange change) {

		final DefaultState current = change.getStart();

		if (current.getIncomingTransition().isEmpty()) { // root?
			return false;
		}

		final DefaultState prev = (DefaultState) current.getIncomingTransition().get().getSource();

		return samePolicy(current, prev) && bothNOOP(change, current.getIncomingTransition().get());
	}

	/**
	 *
	 * @param current
	 * @param prev
	 * @return
	 */
	private static boolean bothNOOP(final ToDoChange current, final RawTransition prev) {
		return current.getChange().isEmpty() && prev.getChange().isEmpty();
	}

	/**
	 *
	 * @param current
	 * @param prev
	 * @return
	 */
	private static boolean samePolicy(final DefaultState current, final DefaultState prev) {
		if (current.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()
				|| prev.getSnapshot().getModelAdjustmentRequestedEvent().isEmpty()) {
			return false;
		}
		final ScalingPolicy policyCurrent = current.getSnapshot().getModelAdjustmentRequestedEvent().get()
				.getScalingPolicy();
		final ScalingPolicy policyPrev = prev.getSnapshot().getModelAdjustmentRequestedEvent().get().getScalingPolicy();

		return policyCurrent.getId().equals(policyPrev.getId());
	}
}
