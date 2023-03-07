package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;

public class DefaultArchitectureConfiguration implements ArchitectureConfiguration {

	private final Allocation alloc;
	private final MonitorRepository monitorRepository;

	public DefaultArchitectureConfiguration(final Allocation alloc, final MonitorRepository monitorRepository) {
		this.alloc = alloc;
		this.monitorRepository = monitorRepository;
	}

	@Override
	public Allocation getAllocation() {
		return this.alloc;
	}

	@Override
	public MonitorRepository getMonitorRepository() {
		return this.monitorRepository;
	}

	/** TODO : I was doing stuff here **/
	@Override
	public ArchitectureConfiguration copy() {
		this.resolveProxies(this.alloc);
		final Allocation copyAlloc = this.copyAlloc(this.alloc);

		final MonitorRepository copyMonitoring = this.copyMonitorRepository(this.monitorRepository);

		return new DefaultArchitectureConfiguration(copyAlloc, copyMonitoring);
	}

	/**
	 *
	 * @param allocation
	 * @return copy of the allocation
	 */
	private Allocation copyAlloc(final Allocation allocation) {

		final ResourceEnvironment resourceEnvironment = allocation.getTargetResourceEnvironment_Allocation();
		final System system = allocation.getSystem_Allocation();

		final String idSegment = UUID.randomUUID().toString();

		final URI oldAllocUri = allocation.eResource().getURI();
		final URI newAllocUri = ResourceUtils.insertFragment(oldAllocUri, idSegment, oldAllocUri.segmentCount() - 1);
		allocation.eResource().setURI(newAllocUri);

		final URI oldResUri = resourceEnvironment.eResource().getURI();
		final URI newResUri = ResourceUtils.insertFragment(oldResUri, idSegment, oldResUri.segmentCount() - 1);
		resourceEnvironment.eResource().setURI(newResUri);

		final URI oldSysUri = system.eResource().getURI();
		final URI newSysUri = ResourceUtils.insertFragment(oldSysUri, idSegment, oldSysUri.segmentCount() - 1);
		system.eResource().setURI(newSysUri);

		ResourceUtils.saveResource(resourceEnvironment.eResource());
		ResourceUtils.saveResource(system.eResource());
		ResourceUtils.saveResource(allocation.eResource());

		//		final Allocation copyAllocUtil = EcoreUtil.copy(this.alloc);
		//		{
		//			final URI oldAllocUriX = this.alloc.eResource().getURI();
		//			final URI newAllocUriX = ResourceUtils.insertFragment(oldAllocUriX, "utilcopy", oldAllocUriX.segmentCount() - 1);
		//
		//			final ResourceSet set = new ResourceSetImpl();
		//			final Resource res = set.createResource(newAllocUriX);
		//			res.getContents().add(copyAllocUtil);
		//			ResourceUtils.saveResource(res);
		//		}

		allocation.eResource().setURI(oldAllocUri);
		system.eResource().setURI(oldSysUri);
		resourceEnvironment.eResource().setURI(oldResUri);

		final ResourceSet set = new ResourceSetImpl();
		final Resource res = set.createResource(newAllocUri);

		try {
			res.load(((XMLResource) res).getDefaultLoadOptions());
		} catch (final IOException e) {
			e.printStackTrace();
		}

		final String allocFragment = res.getURIFragment(allocation);
		final Allocation copy = (Allocation) res.getEObject(allocFragment);

		return copy;
	}

	/**
	 * Resolves system and resource environment proxies by touching once upon each
	 * element.
	 *
	 * TODO if there is an already existing operation to resolve all proxies in some
	 * model, i have simply to found it.
	 *
	 * @param allocation to be resolved
	 */
	private void resolveProxies(final Allocation allocation) {
		this.alloc.getAllocationContexts_Allocation().stream()
		.map(allocContext -> allocContext.getResourceContainer_AllocationContext()).collect(Collectors.toSet());
		this.alloc.getAllocationContexts_Allocation().stream()
		.map(allocContext -> allocContext.getAssemblyContext_AllocationContext()).collect(Collectors.toSet());
	}

	private MonitorRepository copyMonitorRepository(final MonitorRepository monitoring) {
		return monitoring; // TODO
	}

}
