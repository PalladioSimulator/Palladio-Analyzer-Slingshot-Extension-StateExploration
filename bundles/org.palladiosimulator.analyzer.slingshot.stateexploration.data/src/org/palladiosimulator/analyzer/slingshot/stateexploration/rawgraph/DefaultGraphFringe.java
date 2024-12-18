package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.function.Predicate;

import javax.measure.quantity.Force;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
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
public final class DefaultGraphFringe extends ArrayDeque<ToDoChange> {

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
	 *         {@code state} is in the fringe, false otherwise.
	 */
	public boolean containsTodoFor(final RawModelState state, final ScalingPolicy matchee) {
		final Predicate<ToDoChange> pred = todo -> todo.getStart().equals(state)
				&& todo.getChange().isPresent()
				&& todo.getChange().get() instanceof Reconfiguration
				&& ((Reconfiguration) todo.getChange().get()).getAppliedPolicy().getId()
				.equals(matchee.getId());

		return containsTodoFor(pred);
	}

	/**
	 * Check, whether the fringe already contains a {@link ToDoChange} that applies
	 * no reconfiguration to the given state.
	 *
	 * @param state
	 * @return true if a {@link ToDoChange} that without reconfiguration is the
	 *         fringe, false otherwise.
	 */
	public boolean containsNopTodoFor(final RawModelState state) {
		final Predicate<ToDoChange> pred = todo -> todo.getStart().equals(state)
				&& todo.getChange().isEmpty();

		return containsTodoFor(pred);
	}

	private boolean containsTodoFor(final Predicate<ToDoChange> predicate) {
		return this.stream()
				.filter(predicate)
				.findAny()
				.isPresent();
	}

	/**
	 * Remove all {@link ToDoChange}s matching the given criteria from this fringe.
	 *
	 * @param pruningCriteria non-null criteria {@link Force} changes to be removed.
	 */
	public void prune(final Predicate<ToDoChange> pruningCriteria) {
		final Collection<ToDoChange> toBePruned = this.stream().filter(pruningCriteria).toList();

		this.removeAll(toBePruned);

	}


}
