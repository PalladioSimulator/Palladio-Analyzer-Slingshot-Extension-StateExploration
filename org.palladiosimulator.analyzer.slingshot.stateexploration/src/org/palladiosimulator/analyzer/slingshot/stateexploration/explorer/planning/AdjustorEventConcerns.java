package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.Workload;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.TargetGroup;
import de.uka.ipd.sdq.simucomframework.usage.ClosedWorkload;
import de.uka.ipd.sdq.simucomframework.usage.OpenWorkload;

/**
 * To be used during exploration planning.
 *
 * Responsible for things concerned with / related to
 * {@link AdjustorBasedEvent}s.
 *
 * @author stiesssh
 *
 */
public class AdjustorEventConcerns {

	private static final Logger LOGGER = Logger.getLogger(AdjustorEventConcerns.class.getName());

	/**
	 * Create copy of the given event and update TargetGroup to reference the new
	 * copy of the architecture.
	 *
	 * @param event  event to be copied
	 * @param config architecture to be referenced
	 * @return copy of event
	 */
	public DESEvent copyForTargetGroup(final DESEvent event, final ArchitectureConfiguration config) {

		if (event instanceof final ModelAdjustmentRequested adjustor) {
			final ScalingPolicy appliedPolicy = adjustor.getScalingPolicy();
			
			final TargetGroup tg = appliedPolicy.getTargetGroup();

			/* Update Target Group */
			if (tg instanceof final ElasticInfrastructure ei) {
					ei.setUnit(getMatchingResourceContainer(config, ei.getUnit().getId()));
			} else {
				throw new IllegalArgumentException(
						String.format("Target Group of type %s not yet supported", tg.getClass().getSimpleName()));
			}

			/* Create Event copy */
			return new ModelAdjustmentRequested(getMatchingPolicy(config, appliedPolicy));

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
	 * Get a resource container matching the given {@code id} from the new architecture configuration.
	 * 
	 * @param config new copy of the architecture models.
	 * @param id id to match for. 
	 * @return ResourceContainer with the given id from the new config. 
	 * 
	 * @throws NoSuchElementException if the new config has no resource container matching the given {@code id}.
	 */
	private ResourceContainer getMatchingResourceContainer(final ArchitectureConfiguration config,
			String id) {
		Optional<ResourceContainer> newrc = config.getAllocation().getTargetResourceEnvironment_Allocation()
				.getResourceContainer_ResourceEnvironment().stream()
				.filter(rc -> rc.getId().equals(id)).findAny();
		if (newrc.isEmpty()) {
			throw new NoSuchElementException(String.format(
					"No ResourceContainer matching ID %s in new Architectur Configuration.", id));			
		} 
		return newrc.get();
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