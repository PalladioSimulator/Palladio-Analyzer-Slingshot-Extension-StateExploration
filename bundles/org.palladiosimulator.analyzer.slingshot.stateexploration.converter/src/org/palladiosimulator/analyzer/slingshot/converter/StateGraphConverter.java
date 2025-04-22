package org.palladiosimulator.analyzer.slingshot.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.converter.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.converter.data.StateGraphNode;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 *
 * @author
 *
 */
public class StateGraphConverter {

    /**
     *
     * @param monitorRepository
     * @param expSetting
     * @param sloRepository
     * @param startTime
     * @param endTime
     * @param stateId
     * @param parentId
     * @param scalingPolicies policies in order of appliance.
     * @param converter entire converter for state, because creation differs for managed system and exploration states and this operation cannot distinguis them.
     * @return
     */
	public static StateGraphNode convertState(final Optional<MonitorRepository> monitorRepository,
            final ExperimentSetting expSetting,
            final Optional<ServiceLevelObjectiveRepository> sloRepository, final double startTime, final double endTime, final String stateId, final String parentId,
			final List<ScalingPolicy> scalingPolicies, final MeasurementConverter converter) {
		final List<ServiceLevelObjective> slos = new ArrayList<>();
		List<MeasurementSet> measuremnets = new ArrayList<MeasurementSet>();

		/**
		 * The following lines are needed of the Palladio System to load the Monitors.
		 * This is a workaround because otherwise the reading the monitor of the SLO
		 * MeasurmentDescription for the Measuring Point would be null.
		 */
		if (monitorRepository.isPresent()) {
			for (final Monitor monitor : monitorRepository.get().getMonitors()) {
				// System.out.println(monitor.getEntityName());
			}
		}


		// Add SLOs
		if (sloRepository.isPresent()) {
			for (final ServiceLevelObjective slo : sloRepository.get()
					.getServicelevelobjectives()) {
				slos.add(slo);
			}
		}

		// Add Measurements
		if (expSetting != null) {
		    //final MeasurementConverter converter = new MeasurementConverter(startTime, endTime);
			measuremnets = converter.visitExperiementSetting(expSetting);
		}

		return new StateGraphNode(stateId, startTime, endTime, measuremnets, slos, parentId,
				scalingPolicies);
	}
}
