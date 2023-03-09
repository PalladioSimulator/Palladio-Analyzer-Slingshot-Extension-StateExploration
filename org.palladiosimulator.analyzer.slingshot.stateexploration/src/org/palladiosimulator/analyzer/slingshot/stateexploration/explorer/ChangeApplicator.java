package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.core.api.SimulationInformation;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.change.api.Reconfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ToDoChange;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.Workload;
import de.uka.ipd.sdq.simucomframework.usage.ClosedWorkload;
import de.uka.ipd.sdq.simucomframework.usage.OpenWorkload;

/**
 * Applies change. Currently all change must be Scaling Policies, but might
 * change later on, i guess.
 *
 * @author stiesssh
 *
 */
public class ChangeApplicator {

	/**
	 * Create a new architecture Configuration.
	 *
	 * The new configuration is same as the old one after applying the given policy.
	 *
	 * @param oldConfig previous architecture configuration
	 * @param policy    reconfiguration to be applied
	 * @return a new architecture configuration
	 */
	public ArchitectureConfiguration createNewArchConfig(final RawModelState oldState, final ToDoChange todochange) {
		final ArchitectureConfiguration newArchConfig = oldState.getArchitecureConfiguration().copy();

		if (todochange.getChange().isPresent()) {

			// TODO : maybe i should not use the RawTransitions here, because i *know* for
			// sure that there's no nop transition in the fringe.
			// Bullshit, there are nop transitions in the fringe.
			if (todochange.getChange().get() instanceof Reconfiguration) {

				throw new UnsupportedOperationException("cannot executre Reconfiguration, as we are still missing the SPD extension");

//				final Reconfiguration change = (Reconfiguration) todochange.getChange().get();
//
//				final TriggerContext triggerContext = this.createTriggerContext(newArchConfig.getAllocation(),
//						newArchConfig.getMonitorRepository(), change.getScalingPolicy());
//
//				final AdjustmentResult result = triggerContext.executeTrigger();
//
//				change.setResult(result);
			}
		}
		return newArchConfig;
	}

//	private Set<ModelElementDifference<Entity>> buildDifferences(final AdjustmentResult result) {
//		final Set<ModelElementDifference<Entity>> diff = new HashSet<>();
//
//		for (final ModelChange modelChange : result.getChanges()) {
//
//			switch (modelChange.getModelChangeAction()) {
//			case ADDITION:
//				diff.add(new ModelElementDifference<Entity>(Optional.empty(),
//						Optional.of((Entity) modelChange.getModelElement())));
//				break;
//			case DELETION:
//				diff.add(new ModelElementDifference<Entity>(Optional.of((Entity) modelChange.getModelElement()),
//						Optional.empty()));
//				break;
//			default:
//				break;
//			}
//		}
//
//		return diff;
//	}

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

	/**
	 *
	 * create a new TriggerContext for the given allocation, monitoring and scaling
	 * policy.
	 *
	 * must not use the resource environment from the policy, as that is the
	 * environment on the initial architecture configuration. instead it uses the
	 * environment of the current configuration, accessible through the allocation.
	 *
	 * TODO i'd love to check that the resource environment in the target group of
	 * the scaling rule and the resource environment referenced by the allocation
	 * match. Match as in the allocation resource environment was somehow derived
	 * from the base resource environment from the scaling policy. Match as in, if
	 * the resource environments had ids, the ids would be equal. However, resource
	 * environments have no id, thus i cannot match them by id and i am angray about
	 * that.
	 *
	 * @param allocation        allocation to be reconfigured with the new
	 *                          triggerContext
	 * @param monitorRepository monitoring to be reconfigured with the new
	 *                          triggerContext
	 * @param scalingPolicy     reconfiguration to be applied via the new
	 *                          triggerContext
	 * @return a new trigger context.
	 */
//	private TriggerContext createTriggerContext(final Allocation allocation, final MonitorRepository monitorRepository,
//			final ScalingPolicy scalingPolicy) {
//		Preconditions.checkArgument(scalingPolicy.getTargetGroup() instanceof ElasticInfrastructure,
//				"Unsupported TargetGroup");
//		// sadly, there is no id for ResourceEnvironments :(
//		Preconditions.checkArgument(allocation.getTargetResourceEnvironment_Allocation().getEntityName().equals(
//				((ElasticInfrastructure) scalingPolicy.getTargetGroup()).getPCM_ResourceEnvironment().getEntityName()));
//
//		final TriggerContext.Builder triggerContextBuilder = TriggerContext.builder();
//
//		/* Adjustment Type */
//		final AdjustmentTypeInterpreter adjustmentTypeInterpreter = new AdjustmentTypeInterpreter(
//				this.createEmptySimulationInformation(), allocation, monitorRepository);
//
//		final AdjustmentExecutor adjustmentExecutor = adjustmentTypeInterpreter
//				.doSwitch(scalingPolicy.getAdjustmentType());
//
//		/* create new TargetGroup */
//		final ElasticInfrastructure newTargetGroup = TargetsFactory.eINSTANCE.createElasticInfrastructure();
//		newTargetGroup.setName(scalingPolicy.getTargetGroup().getEntityName());
//		newTargetGroup.setPCM_ResourceEnvironment(allocation.getTargetResourceEnvironment_Allocation());
//
//		/* New Trigger */
//		final ScalingTriggerPredicate scalingTriggerPredicate = ScalingTriggerPredicate.ALWAYS;
//
//		final TriggerContext.Builder contextBuilder = triggerContextBuilder.withAdjustmentExecutor(adjustmentExecutor)
//				.withAdjustmentType(scalingPolicy.getAdjustmentType()).withTargetGroup(newTargetGroup)
//				.withScalingTriggerPredicate(scalingTriggerPredicate)
//				.withScalingTrigger(scalingPolicy.getScalingTrigger());
//
//		/* TODO : Constraints */
//		// final ConstraintInterpreter policyConstraintInterpreter = new
//		// ConstraintInterpreter();
//		// scalingPolicy.getPolicyConstraints().stream()
//		// .map(constraint -> policyConstraintInterpreter.doSwitch(constraint))
//		// .forEach(triggerContextBuilder::withConstraint);
//
//		return contextBuilder.build();
//	}

	/**
	 * Creates an empty simulation information, as the AdjustmentExecutors need the
	 * SimulationInformation to access the simulation time when building their
	 * adjustment results.
	 *
	 * @return an empty SimulationInformation.
	 */
	private SimulationInformation createEmptySimulationInformation() {
		return new SimulationInformation() {
			@Override
			public double currentSimulationTime() {
				return 0;
			}

			@Override
			public int consumedEvents() {
				return 0;
			}
		};
	}
}
