package org.palladiosimulator.analyzer.slingshot.common.utils;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.servicelevelobjective.ServicelevelObjectivePackage;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.SpdPackage;

/**
 * Helper to safely access models from the {@link PCMResourceSetPartition} for
 * which the partition does not provide direct getters.
 *
 * @author stiesssh
 *
 */
public final class PCMResourcePartitionHelper {

	private static final Logger LOGGER = Logger.getLogger(PCMResourcePartitionHelper.class.getName());

	public static MonitorRepository getMonitorRepository(final PCMResourceSetPartition partition) {
		final List<EObject> monitors = partition.getElement(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository());
		if (monitors.size() == 0) {
			LOGGER.info("Monitor not present: List size is 0.");
		}
		return (MonitorRepository) monitors.get(0);
	}

	public static SPD getSPD(final PCMResourceSetPartition partition) {
		final List<EObject> spds = partition.getElement(SpdPackage.eINSTANCE.getSPD());
		if (spds.size() == 0) {
			LOGGER.info("SDP not present: List size is 0.");
		}
		return (SPD) spds.get(0);
	}

	public static ServiceLevelObjectiveRepository getSLORepository(final PCMResourceSetPartition partition) {
		final List<EObject> slos = partition
				.getElement(ServicelevelObjectivePackage.eINSTANCE.getServiceLevelObjectiveRepository());
		if (slos.size() == 0) {
			LOGGER.info("SLOS are not present: List size is 0.");
		}
		return (ServiceLevelObjectiveRepository) slos.get(0);
	}
}
