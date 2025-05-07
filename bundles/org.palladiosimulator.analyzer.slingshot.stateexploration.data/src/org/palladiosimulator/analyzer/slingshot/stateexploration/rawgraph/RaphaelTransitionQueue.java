package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;

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
public final class RaphaelTransitionQueue extends PriorityQueue<PlannedTransition> {
	/**  */
	private static final long serialVersionUID = -698254304773541924L;
	
	private final static Logger LOGGER = Logger.getLogger(PriorityTransitionQueue.class);
	
	static DefaultGraph graph;
	static double limit;
	static int horizon;

	private static Map<Integer, Integer> polled = new HashMap<>();
	
	public RaphaelTransitionQueue(final DefaultGraph graph1, final int maxIterations, final int horizon1, final double md) {
		super(createForUtilityMcts());
		graph = graph1;
		horizon = horizon1;
		limit = maxIterations/ (horizon / md);
		System.out.println(limit);
	}

	@Override
    public PlannedTransition poll() {
		System.out.println("Polling at " + this.size());
        final var res = super.poll();
		polled.put(res.getStart().lenghtOfHistory(), polled.getOrDefault(res.getStart().lenghtOfHistory(), 0) + 1);
		System.out.println("Polled: " + polled);
		System.out.println();
		return res;
    }

	@Override
	public boolean offer(final PlannedTransition e) {
		/*
        var condOpt = e.getChange();
        if(condOpt.isPresent() && condOpt.get() instanceof Reconfiguration) {
            Reconfiguration r = (Reconfiguration) condOpt.get();
            if(r.getReactiveReconfigurationEvents().size() > 1) {
                //System.out.println("Discarding: " + r.getReactiveReconfigurationEvents().size() + " at " + e.getStart().getStartTime());
                return false;
            } else {
                //System.out.println("Keeping: " + r.getReactiveReconfigurationEvents().size() + " at " + e.getStart().getStartTime());
            }
        } else {
            //System.out.println("Cant decide on skip: " + condOpt.orElse(null));
        }
		*/
		return super.offer(e);
	}

	public static Comparator<PlannedTransition> createForUtilityMcts() {
		return new Comparator<PlannedTransition>() {
			@Override
			public int compare(final PlannedTransition o1, final PlannedTransition o2) {
				return -Double.compare(lml(o1), lml(o2));	
			}
			
		};
	}
	
	private static double mcts(final PlannedTransition pt) {
//		System.out.println();
//		System.out.println("NODE: " + pt.getSource().getId());
//		final double n = pt.getStart().getOutgoingTransitions().size();
//		System.out.println("Outgoing Transitions Count (n): " + n);
//		
//		final double u = pt.getStart().totalUtility;
//		System.out.println("Total Utility (u): " + u);
//		
//		final double p = pt.getStart().getIncomingTransition().map(
//				x -> x.getSource().getOutgoingTransitions().size()
//		).orElse(1);
//		System.out.println("Incoming Transition Outgoing Transitions Count (p): " + p);
//		
//		final double d = pt.getStart().totalDuration;
//		System.out.println("Total Duration (d): " + d);
//		
//		final double h = pt.getStart().lenghtOfHistory();
//		System.out.println("Length of History (h): " + h);
//		
//		final double c = Math.sqrt(2);
//		System.out.println("Exploration Constant (c): " + c);
//		
//		final double lambda = 1;
//		System.out.println("Depth Encouragement Factor (lambda): " + lambda);
//		
//		final double utilityPerTime = u / d;
//		System.out.println("Utility Per Time: " + utilityPerTime);
//		
//		final double explorationBonus = c * Math.sqrt(Math.sqrt(n) / p);
//		System.out.println("Exploration Bonus: " + explorationBonus);
//		
//		final double depthEncouragement = lambda * Math.log(d + 1);
//		System.out.println("Depth Encouragement: " + depthEncouragement);
//		
//		final double result = utilityPerTime * h;// + depthEncouragement;
//		System.out.println("Final Computed Value: " + result);
//		
//		return result;
		return 0.0;
	}

	private static double ps(final PlannedTransition pt) {
		int changeMagnitude = 0;
		if(pt.getChange().isPresent() && pt.getChange().get() instanceof Reconfiguration) {
			final Reconfiguration reconf = (Reconfiguration) pt.getChange().get();
			changeMagnitude = reconf.getReactiveReconfigurationEvents().size();
		}
		
		final double u = 0.0; //pt.getStart().totalUtility;
		final double d = 0.0; //pt.getStart().totalDuration;
		final double h = pt.getStart().lenghtOfHistory();
		final double n = graph.outDegreeOf(pt.getSource());;
		
		final double utilityPerTime = u / d;
		final double factor = 1 + Math.exp(-0.5*changeMagnitude);
		
		System.out.println("utilityPerTime: " + utilityPerTime);
		System.out.println("factor: " + factor);
		System.out.println("n: " + n);
		System.out.println("result: " + (utilityPerTime - n) * factor);
		
		return (utilityPerTime - n) * factor;
	}

	private static double lm(final PlannedTransition pt) {
		final double u = 0.0; //pt.getStart().totalUtility;
		final double d = 0.0; //pt.getStart().totalDuration;
		final double h = pt.getStart().lenghtOfHistory();
		
		final var f = pt.getStart().getOutgoingTransitions().size();

		return u / (d * Math.log(f+1));
	}	
	
	private static double lml(final PlannedTransition pt) {
		final double u = 0.0; // pt.getStart().totalUtility;
		final double d = 0.0; // pt.getStart().totalDuration;
		final double h = pt.getStart().lenghtOfHistory();

		final int n = polled.getOrDefault(h, 0);
		
		//var f = pt.getStart().getOutgoingTransitions().size();

		//double avg = polled.values().stream().mapToInt(x -> x).average().orElse(0);

		//double diff = avg - n;

		if(pt.getStart().getDuration() == 0) {
			return -1000;
		}

		final int cs = pt.getChange().map(x -> ((Reconfiguration)x).getAppliedPolicies().size()).orElse(0);
  
    return ((pt.getStart().getUtility() + u)/n) * Math.pow(0.75, cs);
	}
	
	@Override
	public String toString() {
		return this.stream().map(p -> p.getStart().getUtility() + " ").reduce("", (s, t) -> s + t);
	}
}
