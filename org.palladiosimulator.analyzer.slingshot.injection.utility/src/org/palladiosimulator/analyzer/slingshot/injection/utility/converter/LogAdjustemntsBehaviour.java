package org.palladiosimulator.analyzer.slingshot.injection.utility.converter;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelChange;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ResourceEnvironmentChange;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;

/**
 *
 * Behaviour to inject reconfigurations into a simulation run according to a provided plan.
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = ModelAdjusted.class, then = {})
public class LogAdjustemntsBehaviour implements SimulationBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(LogAdjustemntsBehaviour.class);

    /**
     * Announce start of simulation.
     */
    @Subscribe
    public void onModelAdjusted(final ModelAdjusted event) {

        for (final ModelChange<?> change : event.getChanges()) {
            if (change instanceof final ResourceEnvironmentChange resChange) {
                final int old = resChange.getOldResourceContainers()
                    .size();
                final int deleted = resChange.getDeletedResourceContainers()
                    .size();
                final int added = resChange.getNewResourceContainers()
                    .size();

                if (deleted + added != 0) {
                    LOGGER.error(event.time() + " " + old);
                    LOGGER.error(event.time() + " " + (old + added - deleted));
                }
            }
        }
        ;

    }


    @Override
    public boolean isActive() {
        return true;
    }
}
