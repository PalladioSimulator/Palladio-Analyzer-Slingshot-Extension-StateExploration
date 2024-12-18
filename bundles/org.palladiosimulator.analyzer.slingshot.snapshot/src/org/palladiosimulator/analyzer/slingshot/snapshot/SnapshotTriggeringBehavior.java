package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.ElasticInfrastructureCfg;
import org.palladiosimulator.semanticspd.ServiceGroupCfg;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.adjustments.StepAdjustment;
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
public class SnapshotTriggeringBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotTriggeringBehavior.class);

	private final DefaultState state;
	private final SimulationScheduling scheduling;

	private final boolean activated;

	private final Configuration config;

	@Inject
	public SnapshotTriggeringBehavior(final @Nullable DefaultState state, final SimulationScheduling scheduling, final @Nullable Configuration config) {
		this.state = state;
		this.scheduling = scheduling;
		this.config = config;

		this.activated = state != null;
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	@PreIntercept
	public InterceptionResult preInterceptModelAdjustmentRequested(final InterceptorInformation information,
			final ModelAdjustmentRequested event) {
		// only intercept triggered adjustments. do not intercept snapped adjustments..
		// assumption: do not copy adjustor events from the FEL, i.e. the "first" adjustor is always from the snapshot.
		if (event.time() == 0) {
			LOGGER.debug(String.format("Succesfully route %s to %s", event.getName(), information.getEnclosingType().get().getSimpleName()));
			return InterceptionResult.success();
		}

		// keep or delete?
		if (isDrop(event.getScalingPolicy())) {
			return InterceptionResult.success();
		}

		state.setReasonToLeave(ReasonToLeave.reactiveReconfiguration);
		scheduling.scheduleEvent(new SnapshotInitiated(0, event));

		LOGGER.debug(String.format("Abort routing %s to %s", event.getName(), information.getEnclosingType().get().getSimpleName()));
		return InterceptionResult.abort();
	}

	/**
	 *
	 * @param start
	 * @param event
	 * @return
	 */
	private boolean isDrop(final ScalingPolicy policy) {
		if (policy.getAdjustmentType() instanceof final StepAdjustment adjustment
				&& adjustment.getStepValue() < 0) {
			// Scale in!
			final TargetGroup tg = policy.getTargetGroup();
			if (tg instanceof final ElasticInfrastructure ei) {
				final List<ElasticInfrastructureCfg> elements = config.getTargetCfgs().stream()
						.filter(ElasticInfrastructureCfg.class::isInstance)
						.map(ElasticInfrastructureCfg.class::cast)
						.filter(eic -> eic.getUnit().getId().equals(ei.getUnit().getId())).toList();

				if (elements.size() != 1) {
					throw new RuntimeException("Help, wrong number of matching service group configs.");
				}

				return elements.get(0).getElements().size() == 1;
			}

			if (tg instanceof final ServiceGroup sg) {
				final List<ServiceGroupCfg> elements = config.getTargetCfgs().stream()
						.filter(ServiceGroupCfg.class::isInstance)
						.map(ServiceGroupCfg.class::cast)
						.filter(sgc -> sgc.getUnit().getId().equals(sg.getUnitAssembly().getId())).toList();

				if (elements.size() != 1) {
					throw new RuntimeException("Help, wrong number of matching service group configs.");
				}

				return elements.get(0).getElements().size() == 1;
			}

		}
		return false;
	}
}
