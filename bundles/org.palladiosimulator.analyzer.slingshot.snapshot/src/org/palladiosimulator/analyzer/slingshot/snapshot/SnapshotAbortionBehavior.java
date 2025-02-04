package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.semanticspd.CompetingConsumersGroupCfg;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.ElasticInfrastructureCfg;
import org.palladiosimulator.semanticspd.ServiceGroupCfg;
import org.palladiosimulator.semanticspd.TargetGroupCfg;

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

	private final Map<TargetGroupCfg, Integer> tg2size;
	
	private final Configuration config;
	

	@Inject
	public SnapshotAbortionBehavior(final @Nullable DefaultState state,
			final @Nullable EventsToInitOnWrapper eventsWapper, final SimulationScheduling scheduling, final Configuration config) {
		this.state = state;
		this.scheduling = scheduling;
		this.adjustmentEvents =  eventsWapper == null ? null : eventsWapper.getAdjustmentEvents();
		
		this.config = config;
		
		this.tg2size = new HashMap<>();
		
		for (TargetGroupCfg tgcfg : config.getTargetCfgs()) {
			tg2size.put(tgcfg, this.getSizeOf(tgcfg));
		}

		this.activated = state != null && eventsWapper != null;
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}
	
	/**
	 * 
	 * @param tgcfg
	 * @return
	 */
	private int getSizeOf(final TargetGroupCfg tgcfg) {
		if (tgcfg instanceof ElasticInfrastructureCfg ecfg) {
			return ecfg.getElements().size();
		} else if (tgcfg instanceof ServiceGroupCfg scfg) {
			return scfg.getElements().size();
		} else if (tgcfg instanceof CompetingConsumersGroupCfg ccfg) {
			return ccfg.getElements().size();
		} else {
			throw new IllegalArgumentException("TargetGroup of unknown type, cannot determine size of elements.");
		}
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
			for (TargetGroupCfg tgcfg : config.getTargetCfgs()) {
				
				if (tg2size.get(tgcfg) != this.getSizeOf(tgcfg)) {
					return;
				}
			}
			state.addReasonToLeave(ReasonToLeave.aborted);
			scheduling.scheduleEvent(new SnapshotInitiated(0));
		}
	}
}
