package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.annotation.processing.Generated;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.spd.SPD;

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
public class UriBasedArchitectureConfiguration implements ArchitectureConfiguration {


	private final URI usageModel;
	private final URI allocation;
	private final URI resourceEnvironment;
	private final URI system;
	private final URI monitorRepository;
	private final URI measuringPoints;
	private final URI spd;
	private final URI semanticSpdConfigruation;
	private final URI slo;

	private ResourceSet set = null;

	private final List<URI> allURI;

	private UriBasedArchitectureConfiguration(final Allocation allocation,
			final UsageModel usageModel, final MonitorRepository monitorRepository,
			final MeasuringPointRepository measuringpoints, final SPD spd, final Configuration semanticSpdConfigruation,
			final ServiceLevelObjectiveRepository slo) {


		this.allocation = allocation.eResource().getURI();
		this.resourceEnvironment = allocation.getTargetResourceEnvironment_Allocation().eResource().getURI();
		this.system = allocation.getSystem_Allocation().eResource().getURI();

		this.monitorRepository = monitorRepository.eResource().getURI();
		this.measuringPoints = measuringpoints.eResource().getURI();

		this.spd = spd.eResource().getURI();
		this.semanticSpdConfigruation = semanticSpdConfigruation.eResource().getURI();
		this.slo = slo.eResource().getURI();
		this.usageModel = usageModel.eResource().getURI();

		// TODO make it a fucking map, or something like that?
		this.allURI = List.of(this.allocation, this.resourceEnvironment, this.system,
				this.monitorRepository, this.measuringPoints, this.spd, this.semanticSpdConfigruation, this.slo,
				this.usageModel);
	}

	@Override
	public Allocation getAllocation() {
		return this.get(this.allocation);
	}

	@Override
	public MonitorRepository getMonitorRepository() {
		return this.get(this.monitorRepository);
	}

	@Override
	public SPD getSPD() {
		return this.get(this.spd);
	}

	@Override
	public ServiceLevelObjectiveRepository getSLOs() {
		return this.get(this.slo);
	}

	@Override
	public Configuration getSemanticSPDConfiguration() {
		return this.get(this.semanticSpdConfigruation);
	}

	@Override
	public ResourceEnvironment getResourceEnvironment() {
		return this.get(this.resourceEnvironment);
	}

	@Override
	public UsageModel getUsageModel() {
		return this.get(this.usageModel);

	}

	@Override
	public System getSystem() {
		return this.get(this.system);
	}

	@Override
	public MeasuringPointRepository getMeasuringPointRepository() {
		return this.get(this.measuringPoints);
	}

	/**
	 * 
	 * @param <T>
	 * @param uri
	 * @return
	 */
	private final <T> T get(URI uri) {
		if (set == null) {
			this.load();
		}
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
		EcoreUtil.resolveAll(set);
		return (T) res.getContents().get(0);
	}

	/**
	 * create resource set and load all URIs
	 * 
	 * ensure set != null and all proxies are resolved.
	 */
	private void load() {
		this.set = new ResourceSetImpl();

		for (URI uri : allURI) {
			createAndLoadSingleResource(uri, set);
		}
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
	public ArchitectureConfiguration copy() {
		final String idSegment = UUID.randomUUID().toString();

		final Builder builder = UriBasedArchitectureConfiguration.builder();
		// iteration 1 : set ist viel zu voll.
		this.copyAlloc(builder, idSegment);

		final UriBasedArchitectureConfiguration copy = builder.build();

		return copy;
	}

	/**
	 *
	 * @param allocation
	 * @return copy of the allocation
	 */
	private void copyAlloc(final Builder builder, final String idSegment) {

		// 1. get models
		final ResourceEnvironment resourceEnvironmentModel = this.getResourceEnvironment();
		final System systemModel = this.getSystem();
		final Allocation allocModel = this.getAllocation();

		final MonitorRepository monitorRepoModel = this.getMonitorRepository();
		final MeasuringPointRepository measuringPointRepositoryModel = this.getMeasuringPointRepository();
		final ServiceLevelObjectiveRepository sloModel = this.getSLOs();

		final SPD spdModel = this.getSPD();
		final Configuration semanticSpdConfigModel = this.getSemanticSPDConfiguration();

		final UsageModel usageModelModel = this.getUsageModel();

		// 2. update paths
		final URI newAllocUri = ResourceUtils.insertFragment(allocation, idSegment, allocation.segmentCount() - 1);
		allocModel.eResource().setURI(newAllocUri);

		final URI newResUri = ResourceUtils.insertFragment(resourceEnvironment, idSegment,
				resourceEnvironment.segmentCount() - 1);
		resourceEnvironmentModel.eResource().setURI(newResUri);

		final URI newUsageUri = ResourceUtils.insertFragment(usageModel, idSegment, usageModel.segmentCount() - 1);
		usageModelModel.eResource().setURI(newUsageUri);

		final URI newSysUri = ResourceUtils.insertFragment(system, idSegment, system.segmentCount() - 1);
		systemModel.eResource().setURI(newSysUri);

		final URI newSPDUri = ResourceUtils.insertFragment(spd, idSegment, spd.segmentCount() - 1);
		spdModel.eResource().setURI(newSPDUri);

		final URI newSemantiSPDConfigUri = ResourceUtils.insertFragment(semanticSpdConfigruation, idSegment,
				semanticSpdConfigruation.segmentCount() - 1);
		semanticSpdConfigModel.eResource().setURI(newSemantiSPDConfigUri);

		final URI newSLOUri = ResourceUtils.insertFragment(slo, idSegment, slo.segmentCount() - 1);
		sloModel.eResource().setURI(newSLOUri);

		final URI newMeasuringPointRepoUri = ResourceUtils.insertFragment(this.measuringPoints, idSegment,
				this.measuringPoints.segmentCount() - 1);
		measuringPointRepositoryModel.eResource().setURI(newMeasuringPointRepoUri);

		final URI newmonitorRepositoryUri = ResourceUtils.insertFragment(monitorRepository, idSegment,
				monitorRepository.segmentCount() - 1);
		monitorRepoModel.eResource().setURI(newmonitorRepositoryUri);

		// 3. save to new path
		ResourceUtils.saveResource(resourceEnvironmentModel.eResource());
		ResourceUtils.saveResource(systemModel.eResource());
		ResourceUtils.saveResource(allocModel.eResource());
		ResourceUtils.saveResource(usageModelModel.eResource());
		ResourceUtils.saveResource(spdModel.eResource());
		ResourceUtils.saveResource(sloModel.eResource());
		ResourceUtils.saveResource(semanticSpdConfigModel.eResource());

		ResourceUtils.saveResource(measuringPointRepositoryModel.eResource());
		ResourceUtils.saveResource(monitorRepoModel.eResource());

		// 4. reset URIs to old values.
		allocModel.eResource().setURI(allocation);
		usageModelModel.eResource().setURI(usageModel);
		systemModel.eResource().setURI(system);
		resourceEnvironmentModel.eResource().setURI(resourceEnvironment);
		spdModel.eResource().setURI(spd);
		sloModel.eResource().setURI(slo);
		semanticSpdConfigModel.eResource().setURI(semanticSpdConfigruation);

		measuringPointRepositoryModel.eResource().setURI(this.measuringPoints);
		monitorRepoModel.eResource().setURI(this.monitorRepository);

		// 5. load copies
		final ResourceSet newset = new ResourceSetImpl();

		createAndLoadSingleResource(newAllocUri, newset);
		createAndLoadSingleResource(newResUri, newset);
		createAndLoadSingleResource(newSysUri, newset);
		createAndLoadSingleResource(newUsageUri, newset);

		createAndLoadSingleResource(newSPDUri, newset);

		createAndLoadSingleResource(newMeasuringPointRepoUri, newset);
		createAndLoadSingleResource(newmonitorRepositoryUri, newset);

		createAndLoadSingleResource(newSLOUri, newset);
		createAndLoadSingleResource(newSemantiSPDConfigUri, newset);

		EcoreUtil.resolveAll(newset);

		// 6. get copies
		final Allocation copyAlloc = (Allocation) newset.getResource(newAllocUri, true).getContents().get(0);
		final UsageModel copyUsageModel = (UsageModel) newset.getResource(newUsageUri, true).getContents().get(0);
		final SPD copySpd = (SPD) newset.getResource(newSPDUri, true).getContents().get(0);
		final Configuration copySemanticSpdConfig = (Configuration) newset.getResource(newSemantiSPDConfigUri, true)
				.getContents().get(0);

		final MeasuringPointRepository copyMeasuringPoints = (MeasuringPointRepository) newset
				.getResource(newMeasuringPointRepoUri, true).getContents().get(0);

		final MonitorRepository copyMonitoring = (MonitorRepository) newset.getResource(newmonitorRepositoryUri, true)
				.getContents().get(0);
		final ServiceLevelObjectiveRepository copySlo = (ServiceLevelObjectiveRepository) newset
				.getResource(newSLOUri, true).getContents().get(0);

		// 7. build copy with copied mdodels
		builder.withAllocation(copyAlloc);
		builder.withUsageModel(copyUsageModel);
		builder.withSPD(copySpd);
		builder.withSLO(copySlo);
		builder.withMonitorRepository(copyMonitoring);
		builder.withMeasuringPointRepository(copyMeasuringPoints);
		builder.withSemanticSpdConfigruation(copySemanticSpdConfig);
	}

	/**
	 *
	 * @return Builder.
	 */
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
		private MeasuringPointRepository measuringPointRepository;
		private SPD spd;
		private ServiceLevelObjectiveRepository slo;
		private Configuration semanticSpdConfigruation;

		private UsageModel usageModel;

		private Builder() {
		}

		public Builder withAllocation(final Allocation alloc) {
			this.alloc = alloc;
			return this;
		}

		public Builder withUsageModel(final UsageModel usageModel) {
			this.usageModel = usageModel;
			return this;
		}

		public Builder withMonitorRepository(final MonitorRepository monitorRepository) {
			this.monitorRepository = monitorRepository;
			return this;
		}

		public Builder withMeasuringPointRepository(final MeasuringPointRepository measuringPointRepository) {
			this.measuringPointRepository = measuringPointRepository;
			return this;
		}

		public Builder withSPD(final SPD spd) {
			this.spd = spd;
			return this;
		}

		public Builder withSemanticSpdConfigruation(final Configuration semanticSpdConfigruation) {
			this.semanticSpdConfigruation = semanticSpdConfigruation;
			return this;
		}

		public Builder withSLO(final ServiceLevelObjectiveRepository slo) {
			this.slo = slo;
			return this;
		}

		public UriBasedArchitectureConfiguration build() {
			return new UriBasedArchitectureConfiguration(alloc, usageModel, monitorRepository,
					measuringPointRepository, spd, semanticSpdConfigruation, slo);
		}
	}
}
