package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
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
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.targets.CompetingConsumersGroup;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.ServiceGroup;
import org.palladiosimulator.spd.targets.TargetGroup;

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
			final @Nullable EventsToInitOnWrapper eventsWapper, final SimulationScheduling scheduling,
			@Nullable final Configuration config, @Nullable final SPD spd) {
		this.state = state;
		this.scheduling = scheduling;
		this.adjustmentEvents = eventsWapper == null ? null : eventsWapper.getAdjustmentEvents();

		this.config = config;

		this.tg2size = new HashMap<>();

		if (spd != null && config != null) {

			/*
			 * [S3] Consider only target group configs with a matching target group. This is necessary,
			 * because apparently some scale ins reduce the number of assemblies, but not
			 * the number of resource containers. Unclear whether this is a bug or a feature in the SPD transformations.
			 */
			Set<EObject> targetGroups = spd.getTargetGroups().stream().map(tg -> getUnitOf(tg))
					.collect(Collectors.toSet());

			for (TargetGroupCfg tgcfg : config.getTargetCfgs()) {
				if (targetGroups.contains(getUnitOf(tgcfg))) {
					tg2size.put(tgcfg, getSizeOf(tgcfg));
				}
			}
		}

		this.activated = state != null && eventsWapper != null && !this.tg2size.isEmpty();
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	/**
	 * Assumption: all intended reconfiguration happen before further reactive
	 * reconfiguration happen.
	 * 
	 * @param modelAdjusted
	 */
	@Subscribe
	public void onModelAdjusted(final ModelAdjusted modelAdjusted) {
		adjusmentCounter++;

		if (modelAdjusted.time() > 0.0 && adjusmentCounter <= adjustmentEvents.size()) {
			throw new IllegalStateException("Missing model adjusted events from init");
		}

		if (!modelAdjusted.isWasSuccessful()) {
			; // TODO is this the correct behaviour?
		}

		if (adjusmentCounter == adjustmentEvents.size()) {
			LOGGER.debug("Beginn Abortion check for " + state.getId());

			for (TargetGroupCfg tgcfg : config.getTargetCfgs()) {
				if (tg2size.containsKey(tgcfg)) {
					LOGGER.debug(tgcfg.getClass().getSimpleName() + ": old " + tg2size.get(tgcfg) + " new "
							+ getSizeOf(tgcfg));
					if (tg2size.get(tgcfg) != getSizeOf(tgcfg)) {
						return;
					}
				}
			}
			state.addReasonToLeave(ReasonToLeave.aborted);
			scheduling.scheduleEvent(new SnapshotInitiated(0));
			LOGGER.debug("Abort " + state.getId());
		}
	}

	/**
	 * Access helper
	 * 
	 * @param tgcfg
	 * @return
	 */
	private static int getSizeOf(final TargetGroupCfg tgcfg) {
		if (tgcfg instanceof ElasticInfrastructureCfg ecfg) {
			return ecfg.getElements().size();
		} else if (tgcfg instanceof ServiceGroupCfg scfg) {
			return scfg.getElements().size();
		} else if (tgcfg instanceof CompetingConsumersGroupCfg ccfg) {
			return ccfg.getElements().size();
		} else {
			throw new IllegalArgumentException(
					"TargetGroupConfiguration of unknown type, cannot determine size of elements.");
		}
	}

	/**
	 * Access helper
	 * 
	 * @param tgcfg
	 * @return
	 */
	private static EObject getUnitOf(final TargetGroupCfg tgcfg) {
		if (tgcfg instanceof ElasticInfrastructureCfg ecfg) {
			return ecfg.getUnit();
		} else if (tgcfg instanceof ServiceGroupCfg scfg) {
			return scfg.getUnit();
		} else if (tgcfg instanceof CompetingConsumersGroupCfg ccfg) {
			return ccfg.getUnit();
		} else {
			throw new IllegalArgumentException(
					"TargetGroupConfiguration of unknown type, cannot determine size of elements.");
		}
	}

	/**
	 * Access helper
	 * 
	 * @param tg
	 * @return
	 */
	private static EObject getUnitOf(final TargetGroup tg) {
		if (tg instanceof ElasticInfrastructure etg) {
			return etg.getUnit();
		} else if (tg instanceof ServiceGroup stg) {
			return stg.getUnitAssembly();
		} else if (tg instanceof CompetingConsumersGroup ctg) {
			return ctg.getUnitAssembly();
		} else {
			throw new IllegalArgumentException("TargetGroup of unknown type, cannot determine size of elements.");
		}
	}
}
