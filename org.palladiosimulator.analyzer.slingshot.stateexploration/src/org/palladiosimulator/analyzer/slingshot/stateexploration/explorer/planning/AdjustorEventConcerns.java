package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.Workload;
import org.palladiosimulator.spd.ScalingPolicy;

import de.uka.ipd.sdq.simucomframework.usage.ClosedWorkload;
import de.uka.ipd.sdq.simucomframework.usage.OpenWorkload;

/**
 * To be used during exploration planning.
 *
 * Responsible for things concerned with / related to
 * {@link ModelAdjustmentRequested} events.
 *
 * @author stiesssh
 *
 */
public class AdjustorEventConcerns {

	private static final Logger LOGGER = Logger.getLogger(AdjustorEventConcerns.class.getName());

	/**
	 * Create copy of the given event.
	 * 
	 * The copy references a scaling Policy from the given
	 * {@link ArchitectureConfiguration}
	 *
	 * @param event  event to be copied
	 * @param config architecture to be referenced
	 * @return copy of event
	 */
	public DESEvent copyForTargetGroup(final DESEvent event, final ArchitectureConfiguration config) {
		if (event instanceof final ModelAdjustmentRequested adjustor) {
			return new ModelAdjustmentRequested(getMatchingPolicy(config, adjustor.getScalingPolicy()));

		}
		throw new IllegalArgumentException(String.format("Expected DESEvent of type %s, but got %s",
				ModelAdjustmentRequested.class.getSimpleName(), event.getClass().getSimpleName()));
	}


	/**
	 * Get a {@link ScalingPolicy} matching the given {@code appliedPolicy} from the new architecture configuration.
	 * 
	 * @param config new copy of the architecture models.
	 * @param appliedPolicy policy to find the copy of.
	 * @return {@link ScalingPolicy} that is a copy of the given policy.
	 *
	 * @throws NoSuchElementException if the new config has no resource container matching the given {@code id}.
	 */
	private ScalingPolicy getMatchingPolicy(final ArchitectureConfiguration config, final ScalingPolicy appliedPolicy) {
		Optional<ScalingPolicy> copiedPolicy = config.getSPD().getScalingPolicies().stream().filter(policy -> policy.getId().equals(appliedPolicy.getId())).findAny();
		if (copiedPolicy.isEmpty()) {
			throw new NoSuchElementException(String.format(
					"No Scaling Policy matching ID %s in new Architectur Configuration.", appliedPolicy.getId()));
		}
		return copiedPolicy.get();
	}

	/**
	 * what is this even ????
	 *
	 * @param usageModel
	 * @return
	 */
	public UsageModel changeLoad(final UsageModel usageModel) {

		final Workload workload = usageModel.getUsageScenario_UsageModel().get(0).getWorkload_UsageScenario();

		if (workload instanceof OpenWorkload) {
			final OpenWorkload openload = (OpenWorkload) workload;

		} else if (workload instanceof ClosedWorkload) {

			final ClosedWorkload closedload = (ClosedWorkload) workload;

			final PCMRandomVariable var = CoreFactory.eINSTANCE.createPCMRandomVariable();
			var.setSpecification(String.valueOf(5));

			closedload.setThinkTime(var.getSpecification());
		}

		return usageModel;
	}

}