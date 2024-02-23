package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration;

import java.io.IOException;
import java.util.HashMap;
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
 * The further this one evolves, the more it feels like a copy of the
 * resourcePartition thing...
 *
 * Maybe it makes more sense to redesign this.
 *
 * @author stiesssh
 *
 */
public class UriAndSetBasedArchitectureConfiguration
		implements SetBasedArchitectureConfiguration, ArchitectureConfiguration {


	final private Map<EClass, URI> uris;

	private ResourceSet set = null; // for loading the first time. 

	private final String idSegment = UUID.randomUUID().toString();

	private static final Set<EClass> MODEL_ECLASS_WHITELIST = Set.of(RepositoryPackage.eINSTANCE.getRepository(),
			AllocationPackage.eINSTANCE.getAllocation(),
			UsagemodelPackage.eINSTANCE.getUsageModel(), SystemPackage.eINSTANCE.getSystem(),
			ResourceenvironmentPackage.eINSTANCE.getResourceEnvironment(),
			MonitorRepositoryPackage.eINSTANCE.getMonitorRepository(),
			MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository(), SpdPackage.eINSTANCE.getSPD(),
			SemanticspdPackage.eINSTANCE.getConfiguration(),
			ServicelevelObjectivePackage.eINSTANCE.getServiceLevelObjectiveRepository());

	/**
	 * DONT DARE TO FUCKING MOVE THE SET CONTENT. THE SET CONTENT WILL BE USED!! ---
	 * or will it?
	 * 
	 * @param set
	 */
	private UriAndSetBasedArchitectureConfiguration(final Map<EClass, URI> uris) {
		this.uris = uris;

	}

	public UriAndSetBasedArchitectureConfiguration(final ResourceSet set) {

		this.uris = new HashMap<>();

		// TODO : assert that all (and only) "mandatory" model are here?
		for (Resource resource : set.getResources()) {
			if (MODEL_ECLASS_WHITELIST.contains(resource.getContents().get(0).eClass())) {
				uris.put(resource.getContents().get(0).eClass(), resource.getURI());
			}
		}
		System.out.println("breakpointline");
	}


	/**
	 * 
	 * @param <T>
	 * @param uri
	 * @return
	 */
	private final <T> T get(URI uri) {
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
	 */
	private void load() {
		if (set == null) {
			this.set = new ResourceSetImpl();
		}

		// only white listed.
		for (URI uri : uris.values()) {
			createAndLoadSingleResource(uri, set);
		}
		EcoreUtil.resolveAll(set);
	}

	/**
	 * 
	 * create a resource and load the model at the provided uri into that resource.
	 * 
	 * does not resolve proxies.
	 * 
	 * @param uri
	 */
	private void createAndLoadSingleResource(URI uri, ResourceSet newSet) {
		if (newSet.getResource(uri, false) == null || newSet.getResource(uri, false).getContents().isEmpty()) {
			final Resource res = newSet.createResource(uri);
			try {
				res.load(((XMLResource) res).getDefaultLoadOptions());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	/** TODO : I was doing stuff here --- yeah, okey, but what stuff? O_o **/
	@Override
	public UriAndSetBasedArchitectureConfiguration copy() {
		final UriAndSetBasedArchitectureConfiguration copy = this.copyAlloc(idSegment);
		return copy;
	}

	/**
	 *
	 * @param allocation
	 * @return copy of the allocation
	 */
	private UriAndSetBasedArchitectureConfiguration copyAlloc(final String idSegment) {

		this.load(); // 1. load all models. -- maybe it already is loaded? i'm not sure.
		

		final List<Resource> whitelisted = set.getResources().stream()
				.filter(r -> this.shallbeSaved(r.getContents().get(0))).toList();
		final Map<EClass, URI> copyUris = new HashMap<>();

		// 2. update paths
		for (final Resource resource : whitelisted) {
			// cache?
			final URI oldUri = resource.getURI();
			final URI newUri = ResourceUtils.insertFragment(oldUri, idSegment, oldUri.segmentCount() - 1);
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
			final URI oldUri = uris.get(resource.getContents().get(0).eClass()); // should be there, because we
																					// literally just loaded it.
			resource.setURI(oldUri);
		}

		// 5. load copies
		// nope.


		// 6. get copies
		// nope.


		// 7. build copy with copied mdodels
		return new UriAndSetBasedArchitectureConfiguration(copyUris);
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
