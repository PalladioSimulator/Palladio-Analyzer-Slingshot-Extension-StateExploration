package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.SLOModelConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.SLOModelProvider;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;

public class ExplorationModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {
		install(SLOModelConfiguration.class);
		provideModel(ServiceLevelObjective.class, SLOModelProvider.class);
	}

}
