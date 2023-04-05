package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration;

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
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
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
@Deprecated
public class DefaultArchitectureConfiguration implements ArchitectureConfiguration {

	private final Allocation allocation;
	private final MonitorRepository monitorRepository;
	private final SPD spd;

	public DefaultArchitectureConfiguration(final Allocation allocation, final MonitorRepository monitorRepository, final SPD spd) {
		this.allocation = allocation;
		this.monitorRepository = monitorRepository;
		this.spd = spd;
	}

	@Override
	public Allocation getAllocation() {
		return this.allocation;
	}

	@Override
	public MonitorRepository getMonitorRepository() {
		return this.monitorRepository;
	}

	@Override
	public SPD getSPD() {
		return this.spd;
	}

	@Override
	public ServiceLevelObjectiveRepository getSLOs() {
		throw new UnsupportedOperationException("not yet implemented");
	}

	/** TODO : I was doing stuff here --- yeah, okey, but what stuff? O_o **/
	@Override
	public ArchitectureConfiguration copy() {
		this.resolveProxies();
		final String idSegment = UUID.randomUUID().toString();

		final Builder builder = DefaultArchitectureConfiguration.builder();

		this.copyAlloc(builder, idSegment);

		final DefaultArchitectureConfiguration copy = builder.build();
		// copy.resolveProxies(); // Yes, Alloc and SPD reference the same Res.Env once resolved.

		return copy;
	}

	/**
	 *
	 * @param allocation
	 * @return copy of the allocation
	 */
	private void copyAlloc(final Builder builder, final String idSegment) {

		final ResourceEnvironment resourceEnvironment = allocation.getTargetResourceEnvironment_Allocation();
		final System system = allocation.getSystem_Allocation();

		final URI oldAllocUri = allocation.eResource().getURI(); // no resource because i (1) moved it to the partition resource and then (2) removed it there.
		final URI newAllocUri = ResourceUtils.insertFragment(oldAllocUri, idSegment, oldAllocUri.segmentCount() - 1);
		allocation.eResource().setURI(newAllocUri);

		final URI oldResUri = resourceEnvironment.eResource().getURI();
		final URI newResUri = ResourceUtils.insertFragment(oldResUri, idSegment, oldResUri.segmentCount() - 1);
		resourceEnvironment.eResource().setURI(newResUri);

		final URI oldSysUri = system.eResource().getURI();
		final URI newSysUri = ResourceUtils.insertFragment(oldSysUri, idSegment, oldSysUri.segmentCount() - 1);
		system.eResource().setURI(newSysUri);

		final URI oldSPDUri = spd.eResource().getURI();
		final URI newSPDUri = ResourceUtils.insertFragment(oldSPDUri, idSegment, oldSPDUri.segmentCount() - 1);
		spd.eResource().setURI(newSPDUri);

		ResourceUtils.saveResource(resourceEnvironment.eResource());
		ResourceUtils.saveResource(system.eResource());
		ResourceUtils.saveResource(allocation.eResource());
		ResourceUtils.saveResource(spd.eResource());



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
		spd.eResource().setURI(oldSPDUri);


		final ResourceSet set = new ResourceSetImpl();
		final Resource res = set.createResource(newAllocUri);
		final Resource spdres = set.createResource(newSPDUri);

		try {
			res.load(((XMLResource) res).getDefaultLoadOptions());
			spdres.load(((XMLResource) spdres).getDefaultLoadOptions());
		} catch (final IOException e) {
			e.printStackTrace();
		}

		final String allocFragment = res.getURIFragment(allocation);
		final Allocation copyAlloc = (Allocation) res.getEObject(allocFragment);

		final String spdFragment = spdres.getURIFragment(spd);
		final SPD copySpd = (SPD) spdres.getEObject(spdFragment);

		builder.withAllocation(copyAlloc);
		builder.withSPD(copySpd);
		builder.withMonitorRepository(this.monitorRepository); //TODO : copy monitoring
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
		this.allocation.getTargetResourceEnvironment_Allocation();
		this.allocation.getAllocationContexts_Allocation().stream()
		.forEach(allocContext -> allocContext.getResourceContainer_AllocationContext());
		this.allocation.getAllocationContexts_Allocation().stream()
		.forEach(allocContext -> allocContext.getAssemblyContext_AllocationContext());

		this.spd.getTargetGroups().stream().filter(tg -> (tg instanceof ElasticInfrastructure)).map(tg -> (ElasticInfrastructure) tg).forEach(tg -> tg.getPCM_ResourceEnvironment());
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder to build {@link DefaultArchitectureConfiguration}.
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

		public DefaultArchitectureConfiguration build() {
			return new DefaultArchitectureConfiguration(alloc, monitorRepository, spd);
		}
	}

}
