package org.palladiosimulator.analyzer.slingshot.managedsystem.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Singleton;

import org.palladiosimulator.spd.ScalingPolicy;

/**
 * Explored states currently known to the managed system injector.
 *
 * Only contains states for which the following conditions hold:
 *
 * <ul>
 * <li>the state starts with a transformation. In this case the state's
 * {@link StateGraphNode#startTime()} is the point in time of application of the state's
 * {@link StateGraphNode#incomingPolicy()}
 * <li>the state is in the future, compared to the current time of the managed system. Past states
 * will be deleted eventually.
 * </ul>
 *
 * TODO : Clean up handling.
 *
 * @author Sarah Stieß
 *
 */
@Singleton
public final class StatesBlackboard {

    private final Map<String, StateGraphNode> states;

    public StatesBlackboard() {
        super();
        this.states = new HashMap<>();
    }

    public void addState(final StateGraphNode state) {
        if (state.incomingPolicy() != null) {
            states.put(state.id(), state);
        }
    }

    public Optional<ScalingPolicy> getPolicy(final String stateId) {
        if (states.containsKey(stateId)) {
            return Optional.of(states.get(stateId)
                .incomingPolicy());
        }
        return Optional.empty();
    }

    public double getTime(final String stateId) {
        if (!states.containsKey(stateId)) {
            throw new IllegalArgumentException("TODO");
        }
        return states.get(stateId)
            .startTime();
    }

    /**
     * Delete all states, that started at a point in time less or equal to the given point in time.
     *
     * @param pointInTime
     */
    public void cleanUp(final double pointInTime) {
        states.entrySet()
            .removeIf(entry -> entry.getValue()
                .startTime() <= pointInTime);
    }
}
