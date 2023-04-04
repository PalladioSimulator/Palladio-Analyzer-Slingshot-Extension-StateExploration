package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.slingshot.core.extension.ModelProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.servicelevelobjective.ServicelevelObjectivePackage;

@Singleton
public class SLOModelProvider implements ModelProvider<ServiceLevelObjective> {

	private final PCMResourceSetPartitionProvider provider;

	@Inject
	public SLOModelProvider(final PCMResourceSetPartitionProvider provider) {
		this.provider = provider;
	}

	@Override
	public ServiceLevelObjective get() {
		final List<EObject> slos = provider.get().getElement(ServicelevelObjectivePackage.eINSTANCE.getServiceLevelObjective());
		if (slos.size() == 0) {
			throw new IllegalStateException("SLOs not present: List size is 0.");
		}
		return (ServiceLevelObjective) slos.get(0);
	}

}
