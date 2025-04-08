package org.palladiosimulator.analyzer.slingshot.converter;

import java.util.ArrayList;
import java.util.List;

import javax.measure.Measure;

import org.palladiosimulator.analyzer.slingshot.converter.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.converter.data.SLO;
import org.palladiosimulator.analyzer.slingshot.converter.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawModelState;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.spd.ScalingPolicy;

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
	public static StateGraphNode convertState(final RawModelState state, final String parentId,
			final List<ScalingPolicy> scalingPolicies) {
		List<ServiceLevelObjective> slos = new ArrayList<>();
		List<MeasurementSet> measuremnets = new ArrayList<MeasurementSet>();

		/**
		 * The following lines are needed of the Palladio System to load the Monitors.
		 * This is a workaround because otherwise the reading the monitor of the SLO
		 * MeasurmentDescription for the Measuring Point would be null.
		 */
		if (state.getArchitecureConfiguration().getMonitorRepository().isPresent()) {
			for (final Monitor monitor : state.getArchitecureConfiguration().getMonitorRepository().get()
					.getMonitors()) {
				// System.out.println(monitor.getEntityName());
			}
		}
		

		// Add SLOs
		if (state.getArchitecureConfiguration() != null && state.getArchitecureConfiguration().getSLOs().isPresent()) {
			for (final ServiceLevelObjective slo : state.getArchitecureConfiguration().getSLOs().get()
					.getServicelevelobjectives()) {
				slos.add(slo);
			}
		}
		
		// Add Measurements
		if (state.getMeasurements() != null) {
			measuremnets = MeasurementConverter.visitExperiementSetting(state.getMeasurements());
		}

		return new StateGraphNode(state.getId(), state.getStartTime(), state.getEndTime(), measuremnets, slos, parentId,
				scalingPolicies);
	}
}
