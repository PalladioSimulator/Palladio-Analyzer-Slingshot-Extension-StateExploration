package org.palladiosimulator.analyzer.slingshot.managedsystem.utility.converter;

import java.util.ArrayList;
import java.util.List;

import org.palladiosimulator.analyzer.slingshot.managedsystem.untility.converter.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.managedsystem.untility.converter.data.SLO;
import org.palladiosimulator.analyzer.slingshot.managedsystem.untility.converter.data.StateGraphNode;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;

public class StateGraphConverter {
    public static StateGraphNode convertState(final MonitorRepository monitorRepository,
            final ExperimentSetting expSetting,
            final ServiceLevelObjectiveRepository sloRepository, final double startTime, final double endTime) {
		List<SLO> slos = new ArrayList<SLO>();
		List<MeasurementSet> measuremnets = new ArrayList<MeasurementSet>();

		/**
		 * The following lines are needed of the Palladio System to load the Monitors.
		 * This is a workaround because otherwise the reading the monitor of the SLO
		 * MeasurmentDescription for the Measuring Point would be null.
		 */

        for (final Monitor monitor : monitorRepository.getMonitors()) {
            System.out.println(monitor.getEntityName());
		}


		// Add SLOs
			slos = new ArrayList<SLO>();

            for (final ServiceLevelObjective slo : sloRepository.getServicelevelobjectives()) {
				slos.add(visitServiceLevelObjective(slo));
			}

		// Add Measurements

        measuremnets = MeasurementConverter.visitExperiementSetting(expSetting);

        return new StateGraphNode("", startTime, endTime, measuremnets, slos, "parentId", null);
	}

	public static SLO visitServiceLevelObjective(final ServiceLevelObjective slo) {
		return new SLO(slo.getId(), slo.getName(), slo.getMeasurementSpecification().getId(),
				(Number) slo.getLowerThreshold().getThresholdLimit().getValue(),
				(Number) slo.getUpperThreshold().getThresholdLimit().getValue());
	}
}
