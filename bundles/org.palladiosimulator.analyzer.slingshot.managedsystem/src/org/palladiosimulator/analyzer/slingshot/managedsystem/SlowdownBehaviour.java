package org.palladiosimulator.analyzer.slingshot.managedsystem;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;

/**
 *
 * Preintercept all simulation events and delay them until their time passed.
 *
 * @author Sarah Stieß
 *
 */
public class SlowdownBehaviour implements SimulationBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(SlowdownBehaviour.class);

    private final double factor;

    private double currentTime;

    public SlowdownBehaviour() {
        this.factor = 1.0;
        this.currentTime = 0.0;
    }

    @PreIntercept
    public InterceptionResult preInterceptDESEvent(final InterceptorInformation information, final DESEvent event) {

        final double delay = event.time() - currentTime;

        try {
            Thread.sleep((long) Math.floor(delay * 1000 * factor));
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        this.currentTime = event.time();
        return InterceptionResult.success();
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
