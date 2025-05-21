package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelChange;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ResourceEnvironmentChange;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
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
 * This behaviour is responsible for things that relate to the initial
 * adjustment of a simulation run.
 * 
 * Keep in mind, the initial adjustment are the pro- or reactive adjustments
 * that are scheduled at the beginning of the simulation.
 * 
 * For the handling of adaptations during, respectively at the end of a
 * simulation run, see {@link SnapshotTriggeringBehavior}.
 * 
 * @author Sophie Stieß
 *
 */
@OnEvent(when = ModelAdjusted.class, then = {}, cardinality = EventCardinality.MANY)
public class SnapshotInitialAdjustmentBehaviour implements SimulationBehaviorExtension {

	private static final Logger LOGGER = Logger.getLogger(SnapshotInitialAdjustmentBehaviour.class);

	private final boolean activated;

	/* for saving stuff to file */
	private final Allocation allocation;

	/* for deleting monitors and MP of scaled-in resources */
	private final MonitorRepository monitorrepo;

	@Inject
	public SnapshotInitialAdjustmentBehaviour(final @Nullable EventsToInitOnWrapper eventsWrapper,
			final Allocation allocation, final MonitorRepository monitorrepo) {
		this.allocation = allocation;
		this.monitorrepo = monitorrepo;

		this.activated = eventsWrapper != null;
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	/**
	 * Save models back to file after an adjustment.
	 * 
	 * In case of a scale in, also remove all superfluous monitors and
	 * measuringpoints.
	 * 
	 * We must write the adjusted models back to file to preserve the correct
	 * architecture for future simulation runs.
	 *
	 * @param modelAdjusted adjustment and resulting changes that just happened.
	 */
	@Subscribe
	public void onModelAdjusted(final ModelAdjusted modelAdjusted) {
		for (final ModelChange<?> change : modelAdjusted.getChanges()) {
			if (change instanceof final ResourceEnvironmentChange resEnvChange) {
				for (final ResourceContainer container : resEnvChange.getDeletedResourceContainers()) {
					removeDeletedMonitoring(container);
				}
			}
		}
		ArchitectureConfigurationUtil.saveWhitelisted(this.allocation.eResource().getResourceSet());
	}

	/**
	 * Remove {@link Monitor}s and {@link MeasuringPoint}s that reference the given
	 * {@link ResourceContainer}.
	 * 
	 * Only removes the elements from the loaded models, but does not yet write the
	 * changes back to file.
	 * 
	 * Requires that the given container is already removed from the models, i.e.,
	 * it is already scaled in.
	 *
	 * @note this operation is a workaround, because currently Slingshot never
	 *       removes monitors or measuringpoints. From a functional perspective,
	 *       this is not a problem, but it clutters the models.
	 *
	 * @param deleted {@link ResourceContainer} deleted during scale in.
	 */
	private void removeDeletedMonitoring(final ResourceContainer deleted) {
		final Set<MeasuringPoint> deletedMps = new HashSet<>();

		if (!this.monitorrepo.getMonitors().isEmpty()) {
			final MeasuringPointRepository measuringpointsrepo = this.monitorrepo.getMonitors().get(0)
					.getMeasuringPoint().getMeasuringPointRepository();

			for (final MeasuringPoint mp : Set.copyOf(measuringpointsrepo.getMeasuringPoints())) {
				if (mp instanceof final ResourceContainerMeasuringPoint rcmp
						&& rcmp.getResourceContainer().getId().equals(deleted.getId())) {
					deletedMps.add(rcmp);
					measuringpointsrepo.getMeasuringPoints().remove(rcmp);
				}
			}
		}

		for (final Monitor monitor : Set.copyOf(monitorrepo.getMonitors())) {
			if (deletedMps.contains(monitor.getMeasuringPoint())) {
				monitorrepo.getMonitors().remove(monitor);
			}
		}
	}
}
