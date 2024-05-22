package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.ArrayDeque;

import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * Fringe that manages the {@link ToDoChange}, i.e. all future directions to
 * explore.
 *
 * TODO: Make this something with priority, such that the most interesting
 * {@link ToDoChange} gets polled and thus explored first.
 *
 *
 * @author Sarah Stieß
 *
 */
public class DefaultGraphFringe extends ArrayDeque<ToDoChange> {

	/**
	 *
	 */
	private static final long serialVersionUID = -698254304773541924L;

	/**
	 * Check, whether the fringe already contains a {@link ToDoChange} that applies
	 * {@code matchee} to {@code state}.
	 *
	 * @param state
	 * @param matchee
	 * @return true if a {@link ToDoChange} that applies {@code matchee} to
	 *         {@code state} if in the fringe, false otherwise.
	 */
	public boolean containsTodoFor(final DefaultState state, final ScalingPolicy matchee) {
		return this.stream()
				.filter(todo -> todo.getStart().equals(state)
						&& todo.getChange().isPresent()
						&& todo.getChange().get() instanceof Reconfiguration
						&& ((Reconfiguration) todo.getChange().get()).getAppliedPolicy().getId()
						.equals(matchee.getId()))
				.findAny()
				.isPresent();
	}


}
