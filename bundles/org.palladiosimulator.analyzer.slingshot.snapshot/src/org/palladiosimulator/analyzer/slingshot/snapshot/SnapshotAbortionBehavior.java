package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.AllocationChange;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;

/**
 *
 * Triggers snapshot if a reconfiguration was triggered.
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when = ModelAdjusted.class, then = {})
public class SnapshotAbortionBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotAbortionBehavior.class);

	private final List<ModelAdjustmentRequested> adjustmentEvents;
	
	private int adjusmentCounter = 0;
	
	private final DefaultState state;
	private final SimulationScheduling scheduling;

	private final boolean activated;

	private final Set<AllocationContext> oldContext;
	

	@Inject
	public SnapshotAbortionBehavior(final @Nullable DefaultState state,
			final @Nullable EventsToInitOnWrapper eventsWapper, final SimulationScheduling scheduling, final Allocation allocation) {
		this.state = state;
		this.scheduling = scheduling;
		this.adjustmentEvents =  eventsWapper == null ? null : eventsWapper.getAdjustmentEvents();

		this.oldContext = allocation.getAllocationContexts_Allocation().stream().collect(Collectors.toSet());
		
		this.activated = state != null && eventsWapper != null;
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}
	
	/**
	 * Assumption: all intended reconfiguration happen before further reactive reconfiguration happen. 
	 * 
	 * @param modelAdjusted
	 */
	@Subscribe
	public void onModelAdjusted(final ModelAdjusted modelAdjusted) {
		adjusmentCounter++;
		
		if (!modelAdjusted.isWasSuccessful()) {
			return; // TODO is this the correct behaviour? 
		}
		
		if (adjusmentCounter == adjustmentEvents.size()) {
			Set<AllocationContext> newContext = getContexts(modelAdjusted);
			
			Set<AllocationContext> added = new HashSet<>(newContext);
			added.removeAll(oldContext);
					
			Set<AllocationContext> deleted = new HashSet<>(oldContext);
			deleted.removeAll(newContext);
			
			if (added.isEmpty() && deleted.isEmpty()) {
				state.addReasonToLeave(ReasonToLeave.aborted);
				scheduling.scheduleEvent(new SnapshotInitiated(0));
			}
		}
	}
	
	/**
	 * Extract a set of {@link AllocationContext} from a given {@link ModelAdjusted} event.
	 * 
	 * Requires exactly one {@link AllocationChange} among the changes. 
	 * 
	 * @param modelAdjusted
	 * @return set of {@link AllocationContext}
	 */
	private Set<AllocationContext> getContexts(final ModelAdjusted modelAdjusted) {
		assert modelAdjusted.isWasSuccessful();
		
		List<AllocationChange> change = modelAdjusted.getChanges().stream().filter(AllocationChange.class::isInstance).map(AllocationChange.class::cast).toList();
		
		if (change.size() == 1) {
			return change.get(0).getObject().getAllocationContexts_Allocation().stream().collect(Collectors.toSet());
		} else {
			throw new IllegalArgumentException(String.format("Expected exactly 1 AllocationChange, but found %d", change.size()));
		}
		
	}
}
