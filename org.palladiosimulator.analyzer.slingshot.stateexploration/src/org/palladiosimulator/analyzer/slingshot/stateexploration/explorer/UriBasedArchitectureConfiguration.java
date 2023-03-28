package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.io.IOException;
import java.util.UUID;
import javax.annotation.processing.Generated;

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
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;

/**
 *
 * The further this one evolves, the more it feels like a copy of the resourcePartition thing...
 *
 * Maybe it makes more sense to redesign this.
 *
 * @author stiesssh
 *
 */
public class UriBasedArchitectureConfiguration implements ArchitectureConfiguration {

	private final URI allocation;
	private final URI monitorRepository;
	private final URI spd;

	private ResourceSet set = null;

	public UriBasedArchitectureConfiguration(final Allocation allocation, final MonitorRepository monitorRepository, final SPD spd) {
		this.allocation = allocation.eResource().getURI();
		this.monitorRepository = monitorRepository.eResource().getURI();
		this.spd = spd.eResource().getURI();
	}

	@Override
	public Allocation getAllocation() {
		if (set == null) {
			this.load();
		}
		if (set.getResource(allocation, false).getContents().isEmpty()) {
			final Resource tmp = set.getResource(allocation, false);
			try {
				tmp.unload();
				tmp.load(((XMLResource) tmp).getDefaultLoadOptions());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return (Allocation) set.getResource(allocation, false).getContents().get(0);
	}

	@Override
	public MonitorRepository getMonitorRepository() {
		if (set == null) {
			this.load();
		}
		if (set.getResource(monitorRepository, false).getContents().isEmpty()) {
			final Resource tmp = set.getResource(monitorRepository, false);
			try {
				tmp.unload();
				tmp.load(((XMLResource) tmp).getDefaultLoadOptions());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return (MonitorRepository) set.getResource(monitorRepository, false).getContents().get(0);
	}

	@Override
	public SPD getSPD() {
		if (set == null) {
			this.load();
		}
		if (set.getResource(spd, false).getContents().isEmpty()) {
			final Resource tmp = set.getResource(spd, false);
			try {
				tmp.unload();
				tmp.load(((XMLResource) tmp).getDefaultLoadOptions());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return (SPD) set.getResource(spd, false).getContents().get(0);
	}

	/**
	 * create resource set and load alloc, spd, and monitor
	 */
	private void load() {
		this.set = new ResourceSetImpl();
		final Resource res = set.createResource(allocation);
		final Resource spdres = set.createResource(spd);
		final Resource monitorres = set.createResource(monitorRepository);

		try {
			res.load(((XMLResource) res).getDefaultLoadOptions());
			spdres.load(((XMLResource) spdres).getDefaultLoadOptions());
			monitorres.load(((XMLResource) monitorres).getDefaultLoadOptions());
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/** TODO : I was doing stuff here --- yeah, okey, but what stuff? O_o **/
	@Override
	public ArchitectureConfiguration copy() {
		this.resolveProxies();
		final String idSegment = UUID.randomUUID().toString();

		final Builder builder = UriBasedArchitectureConfiguration.builder();

		this.copyAlloc(builder, idSegment);

		final UriBasedArchitectureConfiguration copy = builder.build();
		// copy.resolveProxies(); // Yes, Alloc and SPD reference the same Res.Env once resolved.

		return copy;
	}

	/**
	 *
	 * @param allocation
	 * @return copy of the allocation
	 */
	private void copyAlloc(final Builder builder, final String idSegment) {

		final SPD spdModel = this.getSPD();
		final Allocation allocModel = this.getAllocation();
		final ResourceEnvironment resourceEnvironment = allocModel.getTargetResourceEnvironment_Allocation();
		final System system = allocModel.getSystem_Allocation();

		final URI newAllocUri = ResourceUtils.insertFragment(allocation, idSegment, allocation.segmentCount() - 1);
		allocModel.eResource().setURI(newAllocUri);

		final URI oldResUri = resourceEnvironment.eResource().getURI();
		final URI newResUri = ResourceUtils.insertFragment(oldResUri, idSegment, oldResUri.segmentCount() - 1);
		resourceEnvironment.eResource().setURI(newResUri);

		final URI oldSysUri = system.eResource().getURI();
		final URI newSysUri = ResourceUtils.insertFragment(oldSysUri, idSegment, oldSysUri.segmentCount() - 1);
		system.eResource().setURI(newSysUri);

		final URI newSPDUri = ResourceUtils.insertFragment(spd, idSegment, spd.segmentCount() - 1);
		spdModel.eResource().setURI(newSPDUri);

		ResourceUtils.saveResource(resourceEnvironment.eResource());
		ResourceUtils.saveResource(system.eResource());
		ResourceUtils.saveResource(allocModel.eResource());
		ResourceUtils.saveResource(spdModel.eResource());



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

		allocModel.eResource().setURI(allocation);
		system.eResource().setURI(oldSysUri);
		resourceEnvironment.eResource().setURI(oldResUri);
		spdModel.eResource().setURI(spd);


		final ResourceSet newset = new ResourceSetImpl();
		final Resource res = newset.createResource(newAllocUri);
		final Resource spdres = newset.createResource(newSPDUri);

		try {
			res.load(((XMLResource) res).getDefaultLoadOptions());
			spdres.load(((XMLResource) spdres).getDefaultLoadOptions());
		} catch (final IOException e) {
			e.printStackTrace();
		}

		final String allocFragment = res.getURIFragment(allocModel);
		final Allocation copyAlloc = (Allocation) res.getEObject(allocFragment);

		final String spdFragment = spdres.getURIFragment(spdModel);
		final SPD copySpd = (SPD) spdres.getEObject(spdFragment);

		builder.withAllocation(copyAlloc);
		builder.withSPD(copySpd);
		builder.withMonitorRepository(this.getMonitorRepository()); //TODO : copy monitoring
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
	public void resolveProxies() {
		this.getAllocation().getTargetResourceEnvironment_Allocation();
		this.getAllocation().getAllocationContexts_Allocation().stream()
		.forEach(allocContext -> allocContext.getResourceContainer_AllocationContext());
		this.getAllocation().getAllocationContexts_Allocation().stream()
		.forEach(allocContext -> allocContext.getAssemblyContext_AllocationContext());

		this.getSPD().getTargetGroups().stream().filter(tg -> (tg instanceof ElasticInfrastructure)).map(tg -> (ElasticInfrastructure) tg).forEach(tg -> tg.getPCM_ResourceEnvironment());
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder to build {@link UriBasedArchitectureConfiguration}.
	 */
	@Generated("SparkTools")
	public static final class Builder {
		private Allocation alloc;
		private MonitorRepository monitorRepository;
		private SPD spd;

		private Builder() {
		}

		public Builder withAllocation(final Allocation alloc) {
			this.alloc = alloc;
			return this;
		}

		public Builder withMonitorRepository(final MonitorRepository monitorRepository) {
			this.monitorRepository = monitorRepository;
			return this;
		}

		public Builder withSPD(final SPD spd) {
			this.spd = spd;
			return this;
		}

		public UriBasedArchitectureConfiguration build() {
			return new UriBasedArchitectureConfiguration(alloc, monitorRepository, spd);
		}
	}

}
