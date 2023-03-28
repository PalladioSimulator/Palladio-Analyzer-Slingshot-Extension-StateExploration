package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.AdjustorBasedEvent;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.StepBasedAdjustor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.Workload;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.TargetGroup;
import org.palladiosimulator.spd.triggers.RelationalOperator;
import org.palladiosimulator.spd.triggers.SimpleFireOnValue;
import org.palladiosimulator.spd.triggers.TriggersFactory;
import org.palladiosimulator.spd.triggers.expectations.ExpectationsFactory;
import org.palladiosimulator.spd.triggers.expectations.ExpectedTime;
import org.palladiosimulator.spd.triggers.stimuli.SimulationTime;
import org.palladiosimulator.spd.triggers.stimuli.StimuliFactory;

import de.uka.ipd.sdq.simucomframework.usage.ClosedWorkload;
import de.uka.ipd.sdq.simucomframework.usage.OpenWorkload;

/**
 *
 *
 *
 * @author stiesssh
 *
 */
public class ChangeApplicator {

	private static final Logger LOGGER = Logger.getLogger(ChangeApplicator.class.getName());


	/**
	 * Create copy of the given event and update TargetGroup to reference the new copy of the architecture.
	 *
	 * @param event event to be copied
	 * @param config architecture to be referenced
	 * @return copy of event
	 */
	public DESEvent updateTargetGroup(final DESEvent event, final ArchitectureConfiguration config){

		if (event instanceof final AdjustorBasedEvent adjustor) {
			final TargetGroup tg = adjustor.getTargetGroup();

			/* Update Target Group */
			if (tg instanceof final ElasticInfrastructure ei) {
				ei.setPCM_ResourceEnvironment(config.getAllocation().getTargetResourceEnvironment_Allocation());
			} else {
				throw new IllegalArgumentException(String.format("Target Group of type %s not yet supported", tg.getClass().getSimpleName()));
			}

			/*Create Event copy*/
			if (event instanceof final StepBasedAdjustor specificAdjustor) {
				return new StepBasedAdjustor(tg, specificAdjustor.getStepCount());
			} else {
				throw new IllegalArgumentException(String.format("Adjustor event of type %s not yet supported", event.getClass().getSimpleName()));
			}
		}
		throw new IllegalArgumentException(String.format("Expected DESEvent of type %s, but got %s", AdjustorBasedEvent.class.getSimpleName(), event.getClass().getSimpleName()));
	}

	/**
	 * create new scaling policy with trigger on simulation time value.
	 * triggers (proactive) reconfiguration at t = 0.
	 *
	 * @param template
	 * @param config
	 * @return
	 */
	public ScalingPolicy createOneTimeUsageScalingPolicy(final ScalingPolicy template, final ArchitectureConfiguration config){
		final ScalingPolicy oneTrickPony = EcoreUtil.copy(template);//SpdFactory.eINSTANCE.createScalingPolicy();


		final ExpectedTime time = ExpectationsFactory.eINSTANCE.createExpectedTime();
		time.setValue(0.0);
		final SimulationTime stimulus = StimuliFactory.eINSTANCE.createSimulationTime();

		final SimpleFireOnValue trigger = TriggersFactory.eINSTANCE.createSimpleFireOnValue();
		trigger.setExpectedValue(time);
		trigger.setStimulus(stimulus);
		trigger.setRelationalOperator(RelationalOperator.GREATER_THAN_OR_EQUAL_TO);


		oneTrickPony.setScalingTrigger(trigger);

		if (oneTrickPony.getTargetGroup() instanceof final ElasticInfrastructure ei) {
			ei.setPCM_ResourceEnvironment(config.getAllocation().getTargetResourceEnvironment_Allocation());
		} else {
			throw new IllegalArgumentException(String.format("Target Group of type %s not yet supported", oneTrickPony.getTargetGroup().getClass().getSimpleName()));
		}

		return oneTrickPony;
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
