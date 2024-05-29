package org.palladiosimulator.analyzer.slingshot.injection.data;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.palladiosimulator.spd.ScalingPolicy;

/**
 * TODO
 *
 * @author Sarah Stieß
 *
 */
public class Plan {

    private final String id;
    private final TreeMap<Double, Set<ScalingPolicy>> planSteps;

    public Plan(final Map<Double, Set<ScalingPolicy>> planSteps) {
        this.id = UUID.randomUUID()
            .toString();

        this.planSteps = new TreeMap<>(planSteps);
    }

    public boolean isEmpty() {
        return this.planSteps.isEmpty();
    }

    public double getTimeOfNextStep() {
        return this.planSteps.firstKey();
    }

    public Set<ScalingPolicy> getReconfigruations(final Double time) {
        return this.planSteps.get(time);
    }

    /**
     *
     * @param time
     */
    public void forwardPlanTo(final Double time) {
        while (!this.isEmpty() && this.planSteps.firstKey() <= time) {
            this.planSteps.remove(this.planSteps.firstKey());
        }
    }

    public String getId() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);// , planSteps);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Plan other = (Plan) obj;
        return Objects.equals(id, other.id);// && Objects.equals(planSteps, other.planSteps);
    }
}
