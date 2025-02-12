package org.palladiosimulator.analyzer.slingshot.managedsystem;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PostIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.Link;

/**
 *
 * TODO
 *
 * @author Sophie Stieß
 *
 */
public class RLInjectionSimulationBehaviour implements SimulationBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(RLInjectionSimulationBehaviour.class);

    private final Link linkToSystem;

    @Inject
    public RLInjectionSimulationBehaviour(final Link link, final SimulationScheduling scheduling) {
        this.linkToSystem = link;
        this.linkToSystem.setScheduling(scheduling);
    }

    /**
     * Reset simulation driver, to ensure that no events are posted to the now unused driver.
     */
    @PostIntercept
    public InterceptionResult postInterceptSimulationFinished(final InterceptorInformation interceptionInformation,
            final SimulationFinished event, final Result<?> result) {
        this.linkToSystem.setScheduling(null);
        return InterceptionResult.success();

    }

    @Override
    public boolean isActive() {
        return true;
    }
}
