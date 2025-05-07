package org.palladiosimulator.analyzer.slingshot.stateexploration.fringe;

import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Change;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.ExploredState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.PlannedTransition;

/**
 *
 * Fringe that manages the {@link PlannedTransition}, i.e. all future directions to
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
public final class PriorityTransitionQueue extends PriorityQueue<PlannedTransition> {
	/**  */
	private static final long serialVersionUID = -698254304773541924L;
	
	private final static Logger LOGGER = Logger.getLogger(PriorityTransitionQueue.class);
	
	public PriorityTransitionQueue() {
		super(createForUtilityOnly());
	}

	
	private static Comparator<PlannedTransition> createForUtilityOnly() {
		return new Comparator<PlannedTransition>() {
			@Override
			public int compare(final PlannedTransition o1, final PlannedTransition o2) {
				return -Double.compare(o1.getStart().getUtility(), o2.getStart().getUtility());
				
			}
			
		};
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
	private static Comparator<PlannedTransition> createForUtility() {
		return new Comparator<PlannedTransition>() {

			@Override
			public int compare(final PlannedTransition o1, final PlannedTransition o2) {
				if (compare(o1.getChange(), o2.getChange()) == 0) {
					return -Double.compare(o1.getStart().getUtility(), o2.getStart().getUtility());
				} else {
					return compare(o1.getChange(), o2.getChange());
				}
			}
			

			private int compare(final Optional<Change> c1, final Optional<Change> c2) {
				final int ordinalityMattersThreshold = 2;
				
				int ordinalityDiff = calculateOrinality(c1) - calculateOrinality(c2);
				if (ordinalityDiff < ordinalityMattersThreshold) {
					return 0;
				} else {
					return ordinalityDiff;
				}
			}
			
			private int calculateOrinality(final Optional<Change> c) {
				if (c.isPresent()) {
					return ((Reconfiguration) c.get()).getReactiveReconfigurationEvents().size();			
				} else {
					return 0;
				}
			}
		};
	}
	

	/**
	 * Creates a {@link Comparator} for comparing two instances of
	 * {@link PlannedTransition}.
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
	 * @return comparator for comparing two instances of {@link PlannedTransition}.
	 */
	private static Comparator<PlannedTransition> create() {
		return new Comparator<PlannedTransition>() {

			@Override
			public int compare(final PlannedTransition change1, final PlannedTransition change2) {

				// the longer one is better -> the shorter one is "the least"
				final Comparator<ExploredState> historyLengthComparator = Comparator.comparingInt(s -> s.lenghtOfHistory());

				// the longer one is better -> the shorter one is "the least"
				final Comparator<ExploredState> endTimeComparator = Comparator.comparingDouble(s -> s.getEndTime());

				// the more the better -> the fewer one is "least" (end up with a line, because
				// newest state has always zero out transitions -> "least"
				final Comparator<ExploredState> cardinalityComparator = Comparator
						.comparingInt(s -> s.getOutgoingTransitions().size());

				// the one with NOP shall be "the least"
				final Comparator<PlannedTransition> typeOfChangeComparator = (c1, c2) -> {
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
	
	@Override
	public String toString() {
		return this.stream().map(p -> p.getStart().getUtility() + " ").reduce("", (s, t) -> s + t);
	}
}
