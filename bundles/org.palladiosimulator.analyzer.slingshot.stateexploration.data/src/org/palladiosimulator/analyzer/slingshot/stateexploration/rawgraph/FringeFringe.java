package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.measure.quantity.Force;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * Fringe that manages the {@link PlannedTransition}, i.e. all future directions to
 * explore.
 *
 * Beware: what about duplicates ToDo Changes?
 *
 *
 * @author Sarah Stieß
 *
 */
public final class FringeFringe {

	private final AbstractQueue<PlannedTransition> queuedDate;
	
	private final static Logger LOGGER = Logger.getLogger(FringeFringe.class);
	
	public FringeFringe(final AbstractQueue<PlannedTransition> queue) {
		super();
		this.queuedDate = queue;
	}


	/**
	 * Check, whether the fringe already contains a {@link PlannedTransition} that applies
	 * {@code matchee} to {@code state}.
	 *
	 * @param state
	 * @param matchee
	 * @return true if a {@link PlannedTransition} that applies {@code matchee} to
	 *         {@code state} is in the fringe, false otherwise.
	 */
	public boolean containsTodoFor(final RawModelState state, final ScalingPolicy matchee) {
		final Predicate<PlannedTransition> pred = todo -> todo.getStart().equals(state)
				&& todo.getChange().isPresent()
				&& todo.getChange().get() instanceof Reconfiguration
				&& this.isOutTransitionFor((Reconfiguration) todo.getChange().get(), matchee);

		return containsTodoFor(pred);
	}

	/**
	 * 
	 * @param state
	 * @return
	 */
	public Set<Reconfiguration> getPlannedReconfFor(final RawModelState state) {
		return queuedDate.stream().filter(todo -> todo.getStart().equals(state) && todo.getChange().isPresent())
				.map(todo -> todo.getChange().get())
				.filter(Reconfiguration.class::isInstance)
				.map(Reconfiguration.class::cast)
				.collect(Collectors.toSet());
	}
	
	/**
	 * 
	 * @return
	 */
	public Set<Transition> getAllPlannedTransition() {
		return queuedDate.stream().collect(Collectors.toSet());
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
	 * Check, whether the fringe already contains a {@link PlannedTransition} that applies
	 * no reconfiguration to the given state.
	 *
	 * @param state
	 * @return true if a {@link PlannedTransition} that without reconfiguration is the
	 *         fringe, false otherwise.
	 */
	public boolean containsNopTodoFor(final RawModelState state) {
		final Predicate<PlannedTransition> pred = todo -> todo.getStart().equals(state)
				&& todo.getChange().isEmpty();

		return containsTodoFor(pred);
	}

	/**
	 *
	 * @param predicate
	 * @return true if any todo matches the given predicate, false otherwise.
	 */
	private boolean containsTodoFor(final Predicate<PlannedTransition> predicate) {
		return queuedDate.stream()
				.filter(predicate)
				.findAny()
				.isPresent();
	}

	/**
	 * Remove all {@link PlannedTransition}s matching the given criteria from this fringe.
	 *
	 * @param pruningCriteria non-null criteria {@link Force} changes to be removed.
	 */
	public void prune(final Predicate<PlannedTransition> pruningCriteria) {
		final Collection<PlannedTransition> toBePruned = queuedDate.stream().filter(pruningCriteria).toList();

		queuedDate.removeAll(toBePruned);
	}

	public boolean offer(PlannedTransition e) {
		return this.queuedDate.offer(e);
	}

	public PlannedTransition poll() {
		return this.queuedDate.poll();
	}

	public PlannedTransition peek() {
		return this.queuedDate.peek();
	}

	public Iterator<PlannedTransition> iterator() {
		return this.queuedDate.iterator();
	}

	public int size() {
		return this.queuedDate.size();
	}

	public boolean isEmpty() {
		return this.queuedDate.isEmpty();
	}

}
