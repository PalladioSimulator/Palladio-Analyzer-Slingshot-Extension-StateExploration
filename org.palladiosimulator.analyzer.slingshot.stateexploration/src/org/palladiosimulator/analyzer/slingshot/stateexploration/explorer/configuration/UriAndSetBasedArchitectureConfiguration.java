package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.SetBasedArchitectureConfiguration;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationPackage;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentPackage;
import org.palladiosimulator.pcm.system.SystemPackage;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsagemodelPackage;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.SemanticspdPackage;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.servicelevelobjective.ServicelevelObjectivePackage;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.SpdPackage;

/**
 *
 * A {@link ArchitectureConfiguration} represents a set of PCM models for one
 * state in the state exploration.
 * 
 * For this {@link UriAndSetBasedArchitectureConfiguration}, all PCM models are
 * always persisted to the file system. I.e. this configuration represents (and
 * gives access) to a set of PCM-Models, as they are persisted in the fiel
 * system.
 * 
 *
 * @author stiesssh
 *
 */
public class UriAndSetBasedArchitectureConfiguration
		implements SetBasedArchitectureConfiguration, ArchitectureConfiguration {

	/** contains a mapping for all eClasses in {@code MODEL_ECLASS_WHITELIST} */
	final private Map<EClass, URI> uris;

	private ResourceSet set = null; // for loading the first time.

	private final String idSegment;

	private static final Set<EClass> MODEL_ECLASS_WHITELIST = Set.of(RepositoryPackage.eINSTANCE.getRepository(),
			AllocationPackage.eINSTANCE.getAllocation(), UsagemodelPackage.eINSTANCE.getUsageModel(),
			SystemPackage.eINSTANCE.getSystem(), ResourceenvironmentPackage.eINSTANCE.getResourceEnvironment(),
			MonitorRepositoryPackage.eINSTANCE.getMonitorRepository(),
			MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository(), SpdPackage.eINSTANCE.getSPD(),
			SemanticspdPackage.eINSTANCE.getConfiguration(),
			ServicelevelObjectivePackage.eINSTANCE.getServiceLevelObjectiveRepository());

	/**
	 * Create a new {@code ArchitectureConfiguration}.
	 * 
	 * Beware: Intended to be called by this class' {@code copy} operation only.
	 * 
	 * @param set
	 */
	private UriAndSetBasedArchitectureConfiguration(final Map<EClass, URI> uris, final String idSegment) {
		assert uris.keySet().containsAll(MODEL_ECLASS_WHITELIST) : "Missing EClass mappings";

		this.uris = uris;
		this.idSegment = idSegment;
	}

	/**
	 * Create a new {@code ArchitectureConfiguration} representing the architecture
	 * configuration at the very beginning of the exploration, i.e. the architecture
	 * configuration of the stategraph's root node.
	 * 
	 * @param set
	 * @return a new {@code UriAndSetBasedArchitectureConfiguration}.
	 */
	public static UriAndSetBasedArchitectureConfiguration createRootArchConfig(final ResourceSet set) {
		return new UriAndSetBasedArchitectureConfiguration(set, "root");
	}

	/**
	 * Create a new {@code ArchitectureConfiguration}.
	 * 
	 * @param set
	 * @param idSegment
	 */
	private UriAndSetBasedArchitectureConfiguration(final ResourceSet set, final String idSegment) {
		this.uris = new HashMap<>();
		this.idSegment = idSegment;

		fillUrisMap(set);

	}

	/**
	 * Fills the {@code uris} map of this {@code ArchitectureConfiguration}.
	 * 
	 * Ensures, that {@code uris} contains a mapping for all white listed model
	 * classes.
	 * 
	 * @param set ResourceSet to be filled into {@code uris}.
	 * @throws IllegalArgumentException if any Resource in the given ResourceSet is
	 *                                  empty, or if a mapping for a white listed
	 *                                  model is missing.
	 */
	private void fillUrisMap(final ResourceSet set) {
		for (final Resource resource : set.getResources()) {
			if (resource.getContents().isEmpty()) {
				throw new IllegalArgumentException(
						String.format("Empty resource for : %s.", resource.getURI().toString()));
			}

			if (MODEL_ECLASS_WHITELIST.contains(resource.getContents().get(0).eClass())) {
				uris.put(resource.getContents().get(0).eClass(), resource.getURI());
			}
		}

		if (!uris.keySet().containsAll(MODEL_ECLASS_WHITELIST)) {
			Set<EClass> missingClasses = new HashSet<>(MODEL_ECLASS_WHITELIST);
			missingClasses.removeAll(uris.keySet());

			throw new IllegalArgumentException(
					String.format("Missing EClass mappings for these classes: %s ", missingClasses.toString()));
		}
	}

	/**
	 * 
	 * Get a model from the {@link Resource} with the given URI.
	 * 
	 * @param <T> type of the model
	 * @param uri uri of the {@link Resource} to be accessed.
	 * @return model from the {@link Resource} with the given uri.
	 */
	private final <T> T get(final URI uri) {
		this.load();

		if (set.getResource(uri, false).getContents().isEmpty()) {
			final Resource tmp = set.getResource(uri, false);
			try {
				tmp.unload();
				tmp.load(((XMLResource) tmp).getDefaultLoadOptions());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		Resource res = set.getResource(uri, false);
		EcoreUtil.resolveAll(set); // still needed?
		return (T) res.getContents().get(0);
	}

	/**
	 * create resource set and load all URIs
	 * 
	 * ensure set != null and all proxies are resolved.
	 * 
	 * probably also ensures, that all resources in the set are loaded i.e.
	 * !getContents().isEmpty()
	 * 
	 */
	private void load() {
		if (set == null) {
			this.set = new ResourceSetImpl();
		}

		// only white listed.
		for (URI uri : uris.values()) {
			createAndLoadSingleResource(uri);
		}
		EcoreUtil.resolveAll(set);
	}

	/**
	 * 
	 * Load the contents of the {@link Resource} with the given URI.
	 * 
	 * Creates a new {@link Resource}, if {@code set} contains no resource for the
	 * given URI.
	 * 
	 * If a {@link Resource} for the given URI already exists, and its contents are
	 * not empty, nothing happens.
	 * 
	 * Does not resolve proxies.
	 * 
	 * @param uri
	 */
	private void createAndLoadSingleResource(final URI uri) {
		if (this.set.getResource(uri, false) == null) {
			this.set.createResource(uri);
		}
		if (this.set.getResource(uri, false).getContents().isEmpty()) {
			final Resource res = this.set.getResource(uri, false);
			try {
				res.load(((XMLResource) res).getDefaultLoadOptions());
			} catch (final IOException e) {
				e.printStackTrace();
			}
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
	@Override
	public UriAndSetBasedArchitectureConfiguration copy() {

		final String nextIdSegment = UUID.randomUUID().toString();

		this.load(); // 1. load all models. -- maybe it already is loaded? i'm not sure.

		final List<Resource> whitelisted = set.getResources().stream()
				.filter(r -> this.shallbeSaved(r.getContents().get(0))).toList();
		final Map<EClass, URI> copyUris = new HashMap<>();

		// 2. update paths
		for (final Resource resource : whitelisted) {
			// cache?
			final URI oldUri = resource.getURI();
			final URI newUri = ResourceUtils.insertFragment(oldUri, nextIdSegment, oldUri.segmentCount() - 1);
			resource.setURI(newUri);
			copyUris.put(resource.getContents().get(0).eClass(), newUri);
		}

		// 3. save to new path
		for (final Resource resource : whitelisted) {
			System.out.println("tryinto save" + resource.getURI().toString());
			ResourceUtils.saveResource(resource);
		}

		// 4. reset URIs to old values.
		for (final Resource resource : whitelisted) {
			final URI oldUri = uris.get(resource.getContents().get(0).eClass());
			resource.setURI(oldUri);
		}

		// 7. build copy with copied models
		return new UriAndSetBasedArchitectureConfiguration(copyUris, nextIdSegment);
	}

	@Override
	public List<Resource> getResources() {
		return this.set.getResources();
	}

	@Override
	public ResourceSet getResourceSet() {
		return this.set;
	}

	@Override
	public URI getUri(final EClass type) {
		return uris.get(type);
	}

	@Override
	public String getSegment() {
		return this.idSegment;
	}

	private boolean shallbeSaved(final EObject model) {
		assert model != null;

		return MODEL_ECLASS_WHITELIST.stream().anyMatch(bannedEClass -> bannedEClass == model.eClass());

	}

	@Override
	public SPD getSPD() {
		return get(uris.get(SpdPackage.eINSTANCE.getSPD()));
	}

	@Override
	public Configuration getSemanticSPDConfiguration() {
		return get(uris.get(SemanticspdPackage.eINSTANCE.getConfiguration()));
	}

	@Override
	public ServiceLevelObjectiveRepository getSLOs() {
		return get(uris.get(ServicelevelObjectivePackage.eINSTANCE.getServiceLevelObjectiveRepository()));
	}

	@Override
	public MonitorRepository getMonitorRepository() {
		return get(uris.get(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository()));
	}

	@Override
	public Allocation getAllocation() {
		return get(uris.get(AllocationPackage.eINSTANCE.getAllocation()));
	}

	@Override
	public ResourceEnvironment getResourceEnvironment() {
		return get(uris.get(ResourceenvironmentPackage.eINSTANCE.getResourceEnvironment()));
	}

	@Override
	public org.palladiosimulator.pcm.system.System getSystem() {
		return get(uris.get(SystemPackage.eINSTANCE.getSystem()));
	}

	@Override
	public MeasuringPointRepository getMeasuringPointRepository() {
		return get(uris.get(MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository()));
	}

	@Override
	public UsageModel getUsageModel() {
		return get(uris.get(UsagemodelPackage.eINSTANCE.getUsageModel()));
	}
}
