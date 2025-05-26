package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.SLOModelProvider;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;




public class ExplorationModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {
		provideModel(ServiceLevelObjectiveRepository.class, SLOModelProvider.class);
	}

}
