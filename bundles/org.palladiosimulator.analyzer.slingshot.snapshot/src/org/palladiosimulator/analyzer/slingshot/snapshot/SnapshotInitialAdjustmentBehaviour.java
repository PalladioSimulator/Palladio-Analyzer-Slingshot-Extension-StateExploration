package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelChange;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ResourceEnvironmentChange;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.cost.events.TakeCostMeasurement;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfigurationUtil;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcmmeasuringpoint.ResourceContainerMeasuringPoint;

/**
 *
 * TODO something costs and adjustments
 * 
 * Additional handling in case of starting simulation with an adjustment?
 * 
 * TODO : i think there was a bug wrt. cost in case of starting with adaption.
 * 
 * @author Sophie Stieß
 *
 */
@OnEvent(when = ModelAdjusted.class, then = { TakeCostMeasurement.class }, cardinality = EventCardinality.MANY)
@OnEvent(when = SnapshotInitiated.class, then = { TakeCostMeasurement.class }, cardinality = EventCardinality.MANY)
public class SnapshotInitialAdjustmentBehaviour implements SimulationBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(SnapshotInitialAdjustmentBehaviour.class);

	private final boolean activated;
	private final boolean startWithAdaption;

	/* for saving stuff to file */
	private final Allocation allocation;

	/* for deleting monitors and MP of scaled-in resources */
	private final MonitorRepository monitorrepo;
	private final MeasuringPointRepository measuringpointsrepo;

	/* for handling cost measurements */
	Collection<TakeCostMeasurement> costMeasurementStore = new ArrayList<>();
	private boolean handleCosts = true;

	@Inject
	public SnapshotInitialAdjustmentBehaviour(final @Nullable EventsToInitOnWrapper eventsWrapper, final Allocation allocation,
			final MonitorRepository monitorrepo) {


		this.allocation = allocation;
		this.monitorrepo = monitorrepo;

		if (this.monitorrepo.getMonitors().isEmpty()) {
			this.measuringpointsrepo = null;
		} else {
			this.measuringpointsrepo = this.monitorrepo.getMonitors().get(0).getMeasuringPoint()
					.getMeasuringPointRepository();
		}

		this.startWithAdaption = !eventsWrapper.getAdjustmentEvents().isEmpty();
		this.activated = eventsWrapper != null;
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	/**
	 *
	 * Intercept {@link TakeCostMeasurement} events.
	 *
	 * For two reasons: Firstly, get to know all resources with cost measures to
	 * trigger a measurement in case of a snapshot. Secondly, abort the events, if
	 * the state starts with an adaptation. In this case, cost must only be measured
	 * after the adaptation, c.f.
	 * {@link SnapshotInitialAdjustmentBehaviour#onModelAdjusted(ModelAdjusted)}
	 *
	 *
	 * @param information
	 * @param event
	 * @return success, if this state starts without adaptation, abort other wise.
	 */
	@PreIntercept
	public InterceptionResult preInterceptTakeCostMeasurement(final InterceptorInformation information,
			final TakeCostMeasurement event) {

		if (handleCosts && event.time() == 0) {
			costMeasurementStore.add(event);

			if (startWithAdaption) {
				return InterceptionResult.abort();
			}
		}
		return InterceptionResult.success();

	}

	@Subscribe
	public Result<TakeCostMeasurement> onSnapshotInitiated(final SnapshotInitiated event) {
		return Result.of(costMeasurementStore);
	}

	/**
	 * Update persisted model files, because reconfiguration now happens at runtime,
	 * i.e. not yet propagated to file.
	 *
	 * @param modelAdjusted
	 */
	@Subscribe
	public Result<TakeCostMeasurement> onModelAdjusted(final ModelAdjusted modelAdjusted) {
		for (final ModelChange<?> change : modelAdjusted.getChanges()) {
			if (change instanceof final ResourceEnvironmentChange resEnvChange) {
				for (final ResourceContainer container : resEnvChange.getDeletedResourceContainers()) {
					removeDeletedMonitoring(container);
				}
			}
		}
		ArchitectureConfigurationUtil.saveWhitelisted(this.allocation.eResource().getResourceSet());
		this.handleCosts = false;

		return Result.of(costMeasurementStore);
	}

	/**
	 * Remove Monitors and Measuringpoints that reference the
	 * {@link ResourceContainer} that was deleted during a scale in.
	 *
	 * TODO : danger of NPE!!! 
	 *
	 * @param deleted {@link ResourceContainer} deleted during scale in.
	 */
	private void removeDeletedMonitoring(final ResourceContainer deleted) {

		final Set<MeasuringPoint> deletedMps = new HashSet<>();

		for (final MeasuringPoint mp : Set.copyOf(measuringpointsrepo.getMeasuringPoints())) {
			if (mp instanceof final ResourceContainerMeasuringPoint rcmp
					&& rcmp.getResourceContainer().getId().equals(deleted.getId())) {
				deletedMps.add(rcmp);
				measuringpointsrepo.getMeasuringPoints().remove(rcmp);
			}
		}

		for (final Monitor monitor : Set.copyOf(monitorrepo.getMonitors())) {
			if (deletedMps.contains(monitor.getMeasuringPoint())) {
				monitorrepo.getMonitors().remove(monitor);
			}
		}
	}
}
