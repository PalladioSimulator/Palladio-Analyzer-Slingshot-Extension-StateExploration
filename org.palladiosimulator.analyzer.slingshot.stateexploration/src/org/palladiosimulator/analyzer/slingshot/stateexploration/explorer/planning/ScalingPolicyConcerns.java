package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.SetBasedArchitectureConfiguration;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.semanticspd.CompetingConsumersGroupCfg;
import org.palladiosimulator.semanticspd.ElasticInfrastructureCfg;
import org.palladiosimulator.semanticspd.ServiceGroupCfg;
import org.palladiosimulator.semanticspd.TargetGroupCfg;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.SpdFactory;
import org.palladiosimulator.spd.adjustments.AdjustmentsFactory;
import org.palladiosimulator.spd.adjustments.StepAdjustment;
import org.palladiosimulator.spd.targets.CompetingConsumersGroup;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.ServiceGroup;
import org.palladiosimulator.spd.targets.TargetGroup;
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
	 * TODO
	 *
	 * @param template
	 * @param config
	 * @return
	 */
	public ScalingPolicy createOneTimeUsageScalingPolicy(final ScalingPolicy template,
			final SetBasedArchitectureConfiguration config) {
		if (!this.explorationPolicyTemplates.contains(template)) {
			throw new IllegalArgumentException(
					String.format("ScalingPolicy %s is not a template Policy", template.getEntityName()));
		}

		final ScalingPolicy oneTrickPony = EcoreUtil.copy(template);
		oneTrickPony.setEntityName("OneTrickPonyPolicy");

		Set<TargetGroup> units = getValidTargetGroups(config);

		oneTrickPony.setTargetGroup(units.stream().findAny().orElseThrow(() -> new IllegalArgumentException(
				String.format("No TargetGroup for any unit of the semantic Configs "))));

		return oneTrickPony;
	}

	/**
	 * Get all {@link ResourceContainer} which are unit to a EI target group and
	 * also have a semantic configuration.
	 * 
	 * TODO check whether this is even necessary or SPD already has constraint with
	 * regard to the semantic configs.
	 * 
	 * @param config
	 * @return
	 */
	private Set<TargetGroup> getValidTargetGroups(final SetBasedArchitectureConfiguration config) {
		final Set<Entity> unitOfSemanticConfigurations = config.getSemanticSPDConfiguration().getTargetCfgs().stream()
				.map(conf -> this.getUnits(conf)).collect(Collectors.toSet());

		return config.getSPD().getTargetGroups().stream()
				.filter(tg -> this.matchUnits(tg, unitOfSemanticConfigurations)).collect(Collectors.toSet());
	}

	/**
	 * 
	 * Check whether the {@code unit} of the given {@link TargetGroup} matches a
	 * unit from any semantic Configuration.
	 * 
	 * @param group the target group to be checked
	 * @param units all units from the semantic configuration
	 * @return true, if a matching semantic configuration for the given
	 *         {@link TargetGroup} exists, false otherwise.
	 */
	private boolean matchUnits(TargetGroup group, Set<Entity> units) {
		if (group instanceof ElasticInfrastructure ei) {
			return units.contains(ei.getUnit());
		}
		if (group instanceof ServiceGroup sg) {
			return units.contains(sg.getUnitAssembly());
		}
		if (group instanceof CompetingConsumersGroup ccg) {
			return units.contains(ccg.getUnitAssembly());
		}
		return false;
	}

	/**
	 * 
	 * @param group {@link TargetGroup} whose {@code unit} to get
	 * @return {@code unit} of the given {@link TargetGroup}
	 */
	private Entity getUnits(TargetGroupCfg group) {
		if (group instanceof ElasticInfrastructureCfg ei) {
			return ei.getUnit();
		}
		if (group instanceof ServiceGroupCfg sg) {
			return sg.getUnit();
		}
		if (group instanceof CompetingConsumersGroupCfg ccg) {
			return ccg.getUnit();
		}
		throw new IllegalArgumentException(String.format("Unknown subtype %s of %s", group.getClass().getSimpleName(),
				TargetGroup.class.getSimpleName()));
	}

	/**
	 * Creates a scaling policy from nothing.
	 *
	 * Beware: The target group is not set.
	 *
	 * @return A template Scaling Policy without target group.
	 */
	private ScalingPolicy createTemplatePolicy() {
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

		oneTrickPony.setEntityName("TemplatePolicy");
		oneTrickPony.setActive(true);
		oneTrickPony.setAdjustmentType(adjustment);
		oneTrickPony.setScalingTrigger(trigger);
		// do NOT set the target group
		// oneTrickPony.setTargetGroup(null);

		return oneTrickPony;
	}

}
