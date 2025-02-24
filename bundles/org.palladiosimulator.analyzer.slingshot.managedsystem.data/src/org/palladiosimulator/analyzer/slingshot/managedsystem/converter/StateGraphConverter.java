package org.palladiosimulator.analyzer.slingshot.managedsystem.converter;

import java.util.ArrayList;
import java.util.List;

import org.palladiosimulator.analyzer.slingshot.managedsystem.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.StateGraphNode;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;

/**
 *
 * @author
 *
 */
public class StateGraphConverter {

	/**
	 *
	 *
	 * @param state
	 * @param parentId
	 * @param scalingPolicies policies in order of execution. first policy must be applied at first.
	 * @return
	 */

    public static StateGraphNode convertState(final MonitorRepository monitorRepository,
            final ExperimentSetting expSetting,
            final ServiceLevelObjectiveRepository sloRepository, final double startTime, final double endTime) {
		final List<ServiceLevelObjective> slos = new ArrayList<>();
		List<MeasurementSet> measuremnets = new ArrayList<MeasurementSet>();

		/**
		 * The following lines are needed of the Palladio System to load the Monitors.
		 * This is a workaround because otherwise the reading the monitor of the SLO
		 * MeasurmentDescription for the Measuring Point would be null.
		 */
		for (final Monitor monitor : monitorRepository.getMonitors()) {
				// System.out.println(monitor.getEntityName());
		}


		// Add SLOs
			for (final ServiceLevelObjective slo : sloRepository.getServicelevelobjectives()) {
				slos.add(slo);
			}

		// Add Measurements
		final MeasurementConverter converter = new MeasurementConverter(startTime, endTime);
        measuremnets = converter.visitExperiementSetting(expSetting);

		return new StateGraphNode("", startTime, endTime, measuremnets, slos, "parentId", List.of());
	}
}
