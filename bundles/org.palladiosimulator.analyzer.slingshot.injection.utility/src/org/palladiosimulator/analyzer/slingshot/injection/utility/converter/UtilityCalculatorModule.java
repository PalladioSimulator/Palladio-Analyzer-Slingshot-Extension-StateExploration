package org.palladiosimulator.analyzer.slingshot.injection.utility.converter;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.injection.utility.plotter.LogAdjustemntsBehaviour;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;

/**
 *
 *
 * @author Sophie Stieß
 *
 */
public class UtilityCalculatorModule extends AbstractSlingshotExtension {


    @Override
    protected void configure() {
        install(UtilityCalculatorAndPlotterBehaviour.class);
        install(LogAdjustemntsBehaviour.class);
        install(SLOModelConfiguration.class);
        provideModel(ServiceLevelObjectiveRepository.class, SLOModelProvider.class);
    }
}
