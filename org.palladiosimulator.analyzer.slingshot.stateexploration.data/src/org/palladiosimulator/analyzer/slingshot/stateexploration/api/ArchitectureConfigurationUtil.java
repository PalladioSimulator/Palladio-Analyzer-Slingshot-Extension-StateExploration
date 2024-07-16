package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcm.allocation.AllocationPackage;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentPackage;
import org.palladiosimulator.pcm.system.SystemPackage;
import org.palladiosimulator.pcm.usagemodel.UsagemodelPackage;
import org.palladiosimulator.semanticspd.SemanticspdPackage;
import org.palladiosimulator.servicelevelobjective.ServicelevelObjectivePackage;
import org.palladiosimulator.spd.SpdPackage;
import org.scaledl.usageevolution.UsageevolutionPackage;

/**
 * Util class for the {@link ArchitectureConfiguration}.
 *
 * Provides Helpers for persisting Model from an
 * {@link ArchitectureConfiguration}.
 *
 * @author Sarah Stieß
 *
 */
public class ArchitectureConfigurationUtil {

	private static final Logger LOGGER = Logger.getLogger(ArchitectureConfigurationUtil.class.getName());

	/**
	 * EClasses of all models that must be provided to simulate with Slingshot.
	 */
	public static final Set<EClass> MANDATORY_MODEL_ECLASS = Set.of(RepositoryPackage.eINSTANCE.getRepository(),
			AllocationPackage.eINSTANCE.getAllocation(), UsagemodelPackage.eINSTANCE.getUsageModel(),
			SystemPackage.eINSTANCE.getSystem(), ResourceenvironmentPackage.eINSTANCE.getResourceEnvironment());

	/**
	 * EClasses of all models that are simulated by Slingshot, if provided.
	 */
	public static final Set<EClass> OPTIONAL_MODEL_ECLASSES = Set.of(
			MonitorRepositoryPackage.eINSTANCE.getMonitorRepository(),
			MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository(), SpdPackage.eINSTANCE.getSPD(),
			SemanticspdPackage.eINSTANCE.getConfiguration(),
			ServicelevelObjectivePackage.eINSTANCE.getServiceLevelObjectiveRepository(),
			UsageevolutionPackage.eINSTANCE.getUsageEvolution());

	private static final Set<EClass> MERGED_MODEL_ECLASSES = new HashSet<>();
	static {
		MERGED_MODEL_ECLASSES.addAll(MANDATORY_MODEL_ECLASS);
		MERGED_MODEL_ECLASSES.addAll(OPTIONAL_MODEL_ECLASSES);
	}

	/**
	 * EClasses of all models that maybe persisted as part of the
	 * {@link ArchitectureConfiguration}
	 */
	public static final Set<EClass> MODEL_ECLASS_WHITELIST = Set.copyOf(MERGED_MODEL_ECLASSES);

	/**
	 * Get all {@link Resource}s from the given {@link ResourceSet} that contain a
	 * whitelisted model.
	 *
	 * @param set
	 * @return resources with whitelisted models.
	 */
	public static List<Resource> getWhitelistedModels(final ResourceSet set) {
		return set.getResources().stream().filter(r -> isWhitelisted(r)).toList();
	}

	/**
	 * Checks whether a given model is whitelisted.
	 *
	 * @param model model to be checked, must not be null.
	 * @return true if the model is whitelisted, false otherwise.
	 */
	public static boolean isWhitelisted(final Resource model) {
		return model.getContents().stream()
				.filter(o -> MODEL_ECLASS_WHITELIST.stream().anyMatch(allowedEClass -> allowedEClass == o.eClass()))
				.findAny().isPresent();
	}

	/**
	 * Save all whitelisted resources in the given set to their respective URI.
	 *
	 * @param set
	 */
	public static void saveWhitelisted(final ResourceSet set) {
		final List<Resource> whitelisted = getWhitelistedModels(set);

		for (final Resource resource : whitelisted) {
			LOGGER.debug(String.format("Save resource %s.", resource.getURI().toString()));
			ResourceUtils.saveResource(resource);
		}
	}
}
