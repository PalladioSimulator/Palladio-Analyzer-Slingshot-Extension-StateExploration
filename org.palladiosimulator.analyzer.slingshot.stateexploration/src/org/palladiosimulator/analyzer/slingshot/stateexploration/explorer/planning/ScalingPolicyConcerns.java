package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.SpdFactory;
import org.palladiosimulator.spd.adjustments.AdjustmentsFactory;
import org.palladiosimulator.spd.adjustments.StepAdjustment;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.TargetsFactory;
import org.palladiosimulator.spd.triggers.RelationalOperator;
import org.palladiosimulator.spd.triggers.SimpleFireOnValue;
import org.palladiosimulator.spd.triggers.TriggersFactory;
import org.palladiosimulator.spd.triggers.expectations.ExpectationsFactory;
import org.palladiosimulator.spd.triggers.expectations.ExpectedTime;
import org.palladiosimulator.spd.triggers.stimuli.SimulationTime;
import org.palladiosimulator.spd.triggers.stimuli.StimuliFactory;

/**
 *
 * To be used during exploration planning.
 *
 * Responsible for things concerned with / related to {@link ScalingPolicy}s.
 *
 * @author stiesssh
 *
 */
public class ScalingPolicyConcerns {

	private static final Logger LOGGER = Logger.getLogger(ScalingPolicyConcerns.class.getName());

	private final Set<ScalingPolicy> explorationPolicyTemplates;

	public ScalingPolicyConcerns() {
		this.explorationPolicyTemplates = new HashSet<>();
		this.explorationPolicyTemplates.add(this.createTemplatePolicy());
	}

	public Set<ScalingPolicy> getExplorationPolicyTemplates() {
		return explorationPolicyTemplates;
	}

	/**
	 * create new scaling policy for the given architecture configuration.
	 *
	 * @param template
	 * @param config
	 * @return
	 */
	public ScalingPolicy createOneTimeUsageScalingPolicy(final ScalingPolicy template, final ArchitectureConfiguration config){
		if (!this.explorationPolicyTemplates.contains(template)) {
			throw new IllegalArgumentException(String.format("ScalingPolicy %s is not a template Policy", template.getEntityName()));
		}

		final ScalingPolicy oneTrickPony = EcoreUtil.copy(template);//SpdFactory.eINSTANCE.createScalingPolicy();
		oneTrickPony.setEntityName("OneTrickPonyPolicy");


		if (oneTrickPony.getTargetGroup() instanceof final ElasticInfrastructure ei) {
			ei.setUnit(getTargetGroupUnit(config));
		} else {
			throw new IllegalArgumentException(String.format("Target Group of type %s not yet supported", oneTrickPony.getTargetGroup().getClass().getSimpleName()));
		}

		return oneTrickPony;
	}

	/**
	 * 
	 * Get a {@code unit} {@link ResourceContainer} from the given architecture
	 * configuration.
	 * 
	 * For now, it returns just any container. But once we switch to the new SPD
	 * interpreter with the semantic SPD, we must make sure that we select only RCs
	 * for which we have a semantic model, i guess.
	 * 
	 * TODO fix this.
	 * 
	 * @param config 
	 * @return
	 */
	private ResourceContainer getTargetGroupUnit(final ArchitectureConfiguration config) {
		return config.getAllocation().getTargetResourceEnvironment_Allocation().getResourceContainer_ResourceEnvironment().get(0);
	}

	/**
	 * creates a scaling policy from nothing.
	 *
	 * target group is not set.
	 *
	 * @return
	 */
	private ScalingPolicy createTemplatePolicy(){
		final ScalingPolicy oneTrickPony = SpdFactory.eINSTANCE.createScalingPolicy();

		/* create AdjustmentType */
		final StepAdjustment adjustment = AdjustmentsFactory.eINSTANCE.createStepAdjustment();
		adjustment.setStepValue(1);

		/* create CreateTrigger */
		final ExpectedTime time = ExpectationsFactory.eINSTANCE.createExpectedTime();
		time.setValue(0.0);
		final SimulationTime stimulus = StimuliFactory.eINSTANCE.createSimulationTime();

		final SimpleFireOnValue trigger = TriggersFactory.eINSTANCE.createSimpleFireOnValue();
		trigger.setExpectedValue(time);
		trigger.setStimulus(stimulus);
		trigger.setRelationalOperator(RelationalOperator.EQUAL_TO);


		/* create TargetGroup */
		final ElasticInfrastructure targetGroup = TargetsFactory.eINSTANCE.createElasticInfrastructure();
		//targetGroup.setPCM_ResourceEnvironment(config.getAllocation().getTargetResourceEnvironment_Allocation());

		oneTrickPony.setEntityName("TemplatePolicy");
		oneTrickPony.setActive(true);
		oneTrickPony.setAdjustmentType(adjustment);
		oneTrickPony.setScalingTrigger(trigger);
		oneTrickPony.setTargetGroup(targetGroup);

		return oneTrickPony;
	}
}
