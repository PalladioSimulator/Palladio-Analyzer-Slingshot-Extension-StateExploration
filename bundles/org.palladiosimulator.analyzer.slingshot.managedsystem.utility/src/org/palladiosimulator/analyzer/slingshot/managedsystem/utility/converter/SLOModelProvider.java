package org.palladiosimulator.analyzer.slingshot.managedsystem.utility.converter;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.slingshot.core.extension.ModelProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.servicelevelobjective.ServicelevelObjectivePackage;

/**
 *
 * Provides a {@link ServiceLevelObjective} model instance to the simulation.
 *
 * @author Sarah Stieß
 */
@Singleton
public class SLOModelProvider implements ModelProvider<ServiceLevelObjectiveRepository> {

	private static final Logger LOGGER = Logger.getLogger(SLOModelProvider.class);

	private final PCMResourceSetPartitionProvider provider;

	@Inject
	public SLOModelProvider(final PCMResourceSetPartitionProvider provider) {
		this.provider = provider;
	}

	@Override
	public ServiceLevelObjectiveRepository get() {
		final List<EObject> slos = provider.get()
				.getElement(ServicelevelObjectivePackage.eINSTANCE.getServiceLevelObjectiveRepository());
		if (slos.size() == 0) {
			LOGGER.warn("SLOs not present: List size is 0.");
			return null;
		}
		return (ServiceLevelObjectiveRepository) slos.get(0);
	}

}
