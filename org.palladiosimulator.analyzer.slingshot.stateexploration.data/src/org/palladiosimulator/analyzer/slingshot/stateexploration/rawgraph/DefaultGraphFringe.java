package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Collection;
import java.util.Comparator;
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
public final class DefaultGraphFringe extends PriorityQueue<ToDoChange> {

	private final static Logger LOGGER = Logger.getLogger(DefaultGraphFringe.class);

	public DefaultGraphFringe() {
		super(createForUtility());
	}

	/**
	 * Compare two possible changes by the utility of their start nodes.
	 *
	 * As the head of the queues is the *least* element, the better state (i.e.
	 * higher utility) is considered the lesser.
	 *
	 * @return -1 is the first change has the bigger utility, +1 is the second one
	 *         has the bigger utility, 0 if the utilities are equal.
	 */
	private static Comparator<ToDoChange> createForUtility() {
		return new Comparator<ToDoChange>() {

			@Override
			public int compare(final ToDoChange o1, final ToDoChange o2) {
				return -Double.compare(o1.getStart().getUtility(), o2.getStart().getUtility());
			}
		};
	}

	/**
	 * Creates a {@link Comparator} for comparing two instances of
	 * {@link ToDoChange}.
	 *
	 * Current implementation compares first by path length, then by number of out
	 * going transition, and the by type of transition. The queue prioritises the
	 * least element, thus it is:
	 * <ol>
	 * <li>Long histories are greater than short histories.</li>
	 * <li>More outgoing edges are greater that fewer.</li>
	 * <li>NOP transitions are prioritised over changes.</li>
	 * </ol>
	 *
	 * If 1. is equal, 2. is used. If 2. is equal, 3. is used. If 3. is equal, the
	 * order is arbitrary.
	 *
	 * @return comparator for comparing two instances of {@link ToDoChange}.
	 */
	private static Comparator<ToDoChange> create() {
		return new Comparator<ToDoChange>() {

			@Override
			public int compare(final ToDoChange change1, final ToDoChange change2) {

				// the longer one is better -> the shorter one is "the least"
				final Comparator<DefaultState> historyLengthComparator = Comparator.comparingInt(s -> s.lenghtOfHistory());

				// the longer one is better -> the shorter one is "the least"
				final Comparator<DefaultState> endTimeComparator = Comparator.comparingDouble(s -> s.getEndTime());

				// the more the better -> the fewer one is "least" (end up with a line, because
				// newest state has always zero out transitions -> "least"
				final Comparator<DefaultState> cardinalityComparator = Comparator
						.comparingInt(s -> s.getOutgoingTransitions().size());

				// the one with NOP shall be "the least"
				final Comparator<ToDoChange> typeOfChangeComparator = (c1, c2) -> {
					int rval = 0;
					if (c1.getChange().isEmpty()) {
						rval--;
					}
					if (c2.getChange().isEmpty()) {
						rval++;
					}
					return rval;
				};

				final int historyLength = historyLengthComparator.compare(change1.getStart(), change2.getStart());
				final int endTime = endTimeComparator.compare(change1.getStart(), change2.getStart());
				final int cardinality = cardinalityComparator.compare(change1.getStart(), change2.getStart());
				final int typeOfChange = typeOfChangeComparator.compare(change1, change2);

				final int total = endTime != 0 ? endTime
						: historyLength != 0 ? historyLength : cardinality != 0 ? cardinality : typeOfChange;

				return total;
			}
		};
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

	public Set<Change> getPlannedReconfFor(final RawModelState state) {
		return this.stream().filter(todo -> todo.getStart().equals(state) && todo.getChange().isPresent())
				.map(todo -> todo.getChange().get())
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
