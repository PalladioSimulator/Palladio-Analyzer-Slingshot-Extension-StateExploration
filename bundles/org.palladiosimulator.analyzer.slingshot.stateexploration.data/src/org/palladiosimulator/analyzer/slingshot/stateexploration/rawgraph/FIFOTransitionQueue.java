package org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Iterator;

import org.apache.log4j.Logger;

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
public final class FIFOTransitionQueue extends AbstractQueue<PlannedTransition> {

	ArrayDeque<PlannedTransition> queuedDate = new ArrayDeque<>();
	
	private final static Logger LOGGER = Logger.getLogger(FIFOTransitionQueue.class);
	
	public FIFOTransitionQueue() {
		super();
	}

	@Override
	public boolean offer(PlannedTransition e) {
		return this.queuedDate.offer(e);
	}

	@Override
	public PlannedTransition poll() {
		return this.queuedDate.poll();
	}

	@Override
	public PlannedTransition peek() {
		return this.queuedDate.peek();
	}

	@Override
	public Iterator<PlannedTransition> iterator() {
		return this.queuedDate.iterator();
	}

	@Override
	public int size() {
		this.queuedDate.size();
		return 0;
	}


}
