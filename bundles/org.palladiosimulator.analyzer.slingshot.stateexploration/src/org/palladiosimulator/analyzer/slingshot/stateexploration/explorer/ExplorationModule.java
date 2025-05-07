package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.ExplorerControllerSystemBehaviour;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.ExplorationConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.SLOModelConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.SLOModelProvider;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;




public class ExplorationModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {
		install(ExplorationConfiguration.class);
		install(SLOModelConfiguration.class);
		provideModel(ServiceLevelObjectiveRepository.class, SLOModelProvider.class);
		install(ExplorerControllerSystemBehaviour.class);
	}

}
