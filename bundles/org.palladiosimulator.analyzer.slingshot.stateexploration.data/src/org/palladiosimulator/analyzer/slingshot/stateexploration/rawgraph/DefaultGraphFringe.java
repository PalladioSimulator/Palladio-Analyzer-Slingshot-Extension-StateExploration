package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.measure.quantity.Force;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * Fringe that manages the {@link ToDoChange}, i.e. all future directions to
 * explore.
 *
 * Beware: what about duplicates ToDo Changes?
 *
 * Beware: The head of this queue is the *least* element with respect to the
 * specified ordering. (c.f. JavaDoc of {@link PriorityQueue})
 *
 * @author Sarah Stieß
 *
 */
public final class DefaultGraphFringe extends ArrayDeque<ToDoChange> {

	private final static Logger LOGGER = Logger.getLogger(DefaultGraphFringe.class);
	
	public DefaultGraphFringe() {
		super();
	}

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
				&& this.isOutTransitionFor((Reconfiguration) todo.getChange().get(), matchee);

		return containsTodoFor(pred);
	}

	public Set<Reconfiguration> getPlannedReconfFor(final RawModelState state) {
		return this.stream().filter(todo -> todo.getStart().equals(state) && todo.getChange().isPresent())
				.map(todo -> todo.getChange().get())
				.filter(Reconfiguration.class::isInstance)
				.map(Reconfiguration.class::cast)
				.collect(Collectors.toSet());
	}

	/**
	 *
	 * @param reconf
	 * @param matchee
	 * @return
	 */
	private boolean isOutTransitionFor(final Reconfiguration reconf, final ScalingPolicy matchee) {
		return reconf.getAppliedPolicies().size() == 1 && reconf.getAppliedPolicies().stream().map(p -> p.getId())
				.filter(id -> id.equals(matchee.getId())).count() == 1;
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

	/**
	 *
	 * @param predicate
	 * @return true if any todo matches the given predicate, false otherwise.
	 */
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
