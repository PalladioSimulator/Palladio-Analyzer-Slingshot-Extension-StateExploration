package org.palladiosimulator.analyzer.slingshot.managedsystem;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.Link;
import org.palladiosimulator.analyzer.slingshot.managedsystem.events.PlanUpdated;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.PlanCreatedEventMessage;

/**
 *
 * Delay the Managed System, until it received the first {@link PlanCreatedEventMessage}.
 *
 * For Evaluation purposes. Not intended for use in the wild.
 * In the wild, the {@link SlowdownBehaviour} should be used.
 *
 *
 * @author Sophie Stieß
 *
 */
public class DelayUntilPlanCreatedBehaviour implements SimulationBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(DelayUntilPlanCreatedBehaviour.class);

    final Link link;

    final static int SLEEP_DELAY = 5000;

    @Inject
    public DelayUntilPlanCreatedBehaviour(final Link link) {
        this.link = link;
    }

    /**
     * Put this thread -- and thereby the entire simulation -- to sleed until the first {@link PlanUpdated} event arrived.
     * If none arrives, this is an endless loop.
     *
     * @param information
     * @param event
     * @return
     */
    @PreIntercept
    public InterceptionResult preInterceptSimulationStarted(final InterceptorInformation information,
            final SimulationStarted event) {
        while (!link.hasPlanArrived()) {
            try {
                LOGGER.info("Wait for initial PlanUpdated event.");
                Thread.sleep((long) Math.floor(SLEEP_DELAY));
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
        return InterceptionResult.success();
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
