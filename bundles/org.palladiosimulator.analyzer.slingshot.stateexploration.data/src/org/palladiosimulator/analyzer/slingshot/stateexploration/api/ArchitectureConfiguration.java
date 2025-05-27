package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationPackage;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentPackage;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.system.SystemPackage;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsagemodelPackage;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.SemanticspdPackage;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.servicelevelobjective.ServicelevelObjectivePackage;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.SpdPackage;
import org.scaledl.usageevolution.UsageEvolution;
import org.scaledl.usageevolution.UsageevolutionPackage;

/**
 *
 * A represents a set of PCM models for one
 * state in the state exploration.
 *
 * For this {@link ArchitectureConfiguration}, all PCM models are always
 * persisted to the file system. I.e. this configuration represents (and gives
 * access) to a set of PCM-Models, as they are persisted in the file system.
 *
 *
 * @author Sarah Stieß
 *
 */
public class ArchitectureConfiguration {

	private static final Logger LOGGER = Logger.getLogger(ArchitectureConfiguration.class.getName());


	/** contains a mapping for all eClasses in {@code MODEL_ECLASS_WHITELIST} */
	private final Map<EClass, URI> uris;

	private final ResourceSet set = new ResourceSetImpl();

	private final String idSegment;



	/**
	 * Create a new {@code ArchitectureConfiguration}.
	 *
	 * Beware: Intended to be called by this class' {@code copy} operation only.
	 *
	 * @param uris
	 * @param idSegment
	 */
	private ArchitectureConfiguration(final Map<EClass, URI> uris, final String idSegment) {
		assert uris.keySet().containsAll(ArchitectureConfigurationUtil.MANDATORY_MODEL_ECLASS)
		: String.format(
				"Architecture configuration %s misses URIs for some mandatory models. The configuration has URIs for: %s",
				idSegment, uris.keySet());

		this.uris = uris;
		this.idSegment = idSegment;

	}

	/**
	 * Create a new {@code ArchitectureConfiguration} representing the architecture
	 * configuration at the very beginning of the exploration, i.e. the architecture
	 * configuration of the stategraph's root node, at the given {@code location}.
	 *
	 * Subsequently, all successor nodes are found in subfolders of the provided
	 * location.
	 *
	 * @param set      resources set with models
	 * @param location non-null path to an exiting folder in the file system.
	 * @return a new {@code UriAndSetBasedArchitectureConfiguration}.
	 */
	public static ArchitectureConfiguration createRootArchConfig(final ResourceSet set, final URI location) {
		return copyModelsForRoot(set, location);
	}

	/**
	 *
	 * Create a copy of the models in the provided resource set at the provided
	 * location.
	 *
	 * @param location location of the root node's architecture configuration.
	 * @param set      resources set with models
	 * @return a new {@code UriAndSetBasedArchitectureConfiguration}.
	 */
	private static ArchitectureConfiguration copyModelsForRoot(final ResourceSet set, final URI location) {

		final String nextIdSegment = UUID.randomUUID().toString();
		final String explorationId = EventMessage.EXPLORATION_ID.toString();
		String cleanLocation = location.toString();

		if (location.hasTrailingPathSeparator()) {
			cleanLocation = cleanLocation.substring(0, cleanLocation.length() - 1);
		}

		// 1. ensure that load all models are loaded.
		EcoreUtil.resolveAll(set);

		final List<Resource> whitelisted = ArchitectureConfigurationUtil.getWhitelistedResources(set);
		final Map<EClass, URI> copyUris = new HashMap<>();

		// 2. update paths
		for (final Resource resource : whitelisted) {
			final String file = resource.getURI().lastSegment();

			final URI newUri = URI.createURI(cleanLocation).appendSegment(explorationId).appendSegment(nextIdSegment)
					.appendSegment(file);
			resource.setURI(newUri);
			copyUris.put(resource.getContents().get(0).eClass(), newUri);
		}

		// 3. save to new path (thereby create a copy)
		ArchitectureConfigurationUtil.saveWhitelisted(set);

		// create Arch Config with models at new location
		return new ArchitectureConfiguration(copyUris, nextIdSegment);
	}

	/**
	 * Fills the {@code uris} map of this {@code ArchitectureConfiguration}.
	 *
	 * Ensures, that {@code uris} contains a mapping for all white listed model
	 * classes.
	 *
	 * @param set ResourceSet to be filled into {@code uris}.
	 * @throws IllegalArgumentException if any Resource in the given ResourceSet is
	 *                                  empty.
	 */
	private static Map<EClass, URI> createUriMap(final ResourceSet set) {
		final Map<EClass, URI> map = new HashMap<>();

		for (final Resource resource : set.getResources()) {
			if (resource.getContents().isEmpty()) {
				LOGGER.info(String.format("Empty resource for : %s.", resource.getURI().toString()));
			} else if (ArchitectureConfigurationUtil.MODEL_ECLASS_WHITELIST
					.contains(resource.getContents().get(0).eClass())) {
				map.put(resource.getContents().get(0).eClass(), resource.getURI());
			}
		}

		return map;
	}

	/**
	 * Get a model from the {@link Resource} with the given URI.
	 *
	 * Create a {@link Resource} with the given URI if it does not exist. Load the
	 * resources, if its contents are empty (should not happen).
	 *
	 * Resolves all Proxies in the model by loading referenced models into the
	 * resources.
	 *
	 * @param <T> type of the model
	 * @param uri non-null uri of the {@link Resource} to be accessed.
	 * @return model from the {@link Resource} with the given URI.
	 */
	private final <T> T get(final URI uri) {
		assert uri != null : "Parameter uri is null but must not be.";

		final Resource res = this.set.getResource(uri, true);

		if (res.getContents().isEmpty()) {
			try {
				LOGGER.debug(String.format("Contents of Resource %s was empty and had to be loaded manually.",
						res.getURI().toString()));
				res.unload();
				res.load(((XMLResource) res).getDefaultLoadOptions());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		EcoreUtil.resolveAll(set); // resolve the *set* to load the referenced models.
		return (T) res.getContents().get(0);

	}

	/**
	 * load all URIs
	 *
	 * ensure all proxies are resolved and all resources in the set are loaded i.e.
	 * !getContents().isEmpty()
	 *
	 */
	private void load() {
		for (final URI uri : uris.values()) {
			this.get(uri);
		}
	}

	/**
	 * Creates a copy of this architecture Configuration.
	 *
	 * The copy is created by saving all models of this configuration to a new
	 * location in the file system, and setting the EClass to URI mappings of the
	 * copy such that they are pointing to the copied model in the file system.
	 *
	 */
	public ArchitectureConfiguration copy() {

		final String nextIdSegment = UUID.randomUUID().toString();

		this.load(); // 1. ensure that load all models are loaded.

		final List<Resource> whitelisted = ArchitectureConfigurationUtil.getWhitelistedResources(this.set);
		final Map<EClass, URI> copyUris = new HashMap<>();

		// 2. update paths
		for (final Resource resource : whitelisted) {
			// cache?
			final URI oldUri = resource.getURI();
			final URI newUri = ResourceUtils.replaceFragment(oldUri, nextIdSegment, oldUri.segmentCount() - 2);
			resource.setURI(newUri);
			copyUris.put(resource.getContents().get(0).eClass(), newUri);
		}

		// 3. save to new path
		ArchitectureConfigurationUtil.saveWhitelisted(this.set);

		// 4. reset URIs to old values.
		for (final Resource resource : whitelisted) {
			final URI oldUri = uris.get(resource.getContents().get(0).eClass());
			resource.setURI(oldUri);
		}

		// 7. build copy with copied models
		return new ArchitectureConfiguration(copyUris, nextIdSegment);
	}

	public List<Resource> getResources() {
		return this.set.getResources();
	}

	public URI getUri(final EClass type) {
		return uris.get(type);
	}

	public String getSegment() {
		return this.idSegment;
	}

	public void transferModelsToSet(final ResourceSet set) {
		this.load(); // 1. ensure that load all models are loaded.

		set.getResources().clear();
		set.getResources().addAll(this.getResources());
	}

	public ResourceSet getResourceSet() {
		return this.set;
	}

	public Allocation getAllocation() {
		return get(uris.get(AllocationPackage.eINSTANCE.getAllocation()));
	}

	public ResourceEnvironment getResourceEnvironment() {
		return get(uris.get(ResourceenvironmentPackage.eINSTANCE.getResourceEnvironment()));
	}

	public System getSystem() {
		return get(uris.get(SystemPackage.eINSTANCE.getSystem()));
	}

	public UsageModel getUsageModel() {
		return get(uris.get(UsagemodelPackage.eINSTANCE.getUsageModel()));
	}

	public Repository getRepository() {
		return get(uris.get(RepositoryPackage.eINSTANCE.getRepository()));
	}

	public Optional<SPD> getSPD() {
		if (uris.containsKey(SpdPackage.eINSTANCE.getSPD())) {
			return Optional.of(get(uris.get(SpdPackage.eINSTANCE.getSPD())));
		}
		return Optional.empty();
	}

	public Optional<Configuration> getSemanticSPDConfiguration() {
		if (uris.containsKey(SemanticspdPackage.eINSTANCE.getConfiguration())) {
			return Optional.of(get(uris.get(SemanticspdPackage.eINSTANCE.getConfiguration())));
		}
		return Optional.empty();
	}

	public Optional<ServiceLevelObjectiveRepository> getSLOs() {
		if (uris.containsKey(ServicelevelObjectivePackage.eINSTANCE.getServiceLevelObjectiveRepository())) {
			return Optional
					.of(get(uris.get(ServicelevelObjectivePackage.eINSTANCE.getServiceLevelObjectiveRepository())));
		}
		return Optional.empty();
	}

	public Optional<MonitorRepository> getMonitorRepository() {
		if (uris.containsKey(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository())) {
			return Optional.of(get(uris.get(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository())));
		}
		return Optional.empty();
	}

	public Optional<MeasuringPointRepository> getMeasuringPointRepository() {
		if (uris.containsKey(MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository())) {
			return Optional.of(get(uris.get(MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository())));
		}
		return Optional.empty();
	}

	public Optional<UsageEvolution> getUsageEvolution() {
		if (uris.containsKey(UsageevolutionPackage.eINSTANCE.getUsageEvolution())) {
			return Optional.of(get(uris.get(UsageevolutionPackage.eINSTANCE.getUsageEvolution())));
		}
		return Optional.empty();

	}
}
