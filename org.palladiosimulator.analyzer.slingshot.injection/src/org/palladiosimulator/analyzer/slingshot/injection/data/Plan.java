package org.palladiosimulator.analyzer.slingshot.injection.data;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.common.base.Preconditions;

/**
 * TODO
 *
 * assumption: all plans start at {@code root}
 *
 * @author Sarah Stieß
 *
 */
public class Plan {

    private final String id;
    private final TreeMap<Double, Set<ScalingPolicy>> planSteps;

    private final TreeMap<Double, Set<ScalingPolicy>> executedSteps;

    public Plan(final Map<Double, Set<ScalingPolicy>> planSteps) {
        this.id = UUID.randomUUID()
            .toString();

        this.planSteps = new TreeMap<>(planSteps);
        this.executedSteps = new TreeMap<>();
    }

    /**
     * Determine the duration of common prefix between this plan and the given plan.
     *
     * Requires that the other plan is new, i.e. no steps steps have been executed.
     *
     * Assumes that all plans start at {@code root}. Thus, all already executed steps of this plan
     * must also be part of the other plan.
     *
     *
     * @param other
     *            a new plan
     * @return point in time of the first step, where {@code other} differs from {@code this}, or
     *         {@link Optional#empty()} if there is no common prefix of not executed steps, or if
     *         {@code other} is contained in this plan.
     */
    public Optional<Double> getPointInTimeOfDivergence(final Plan other) {

        Preconditions.checkNotNull(other);
        Preconditions.checkArgument(this.hasCommonHistory(other),
                String.format("Given plan %s does not contain the history of executed steps of this plan %s",
                        other.getId(), this.getId()));

        other.forwardPlanTo(this.getTimeOfNextStep());

        final Iterator<Entry<Double, Set<ScalingPolicy>>> itOther = new TreeMap<>(other.getPlanSteps()).entrySet()
            .iterator();

        final Iterator<Entry<Double, Set<ScalingPolicy>>> itPlanned = this.planSteps.entrySet()
            .iterator();

        while (itPlanned.hasNext() && itOther.hasNext()) {

            final Entry<Double, Set<ScalingPolicy>> planned = itPlanned.next();
            final Entry<Double, Set<ScalingPolicy>> otherStep = itOther.next();

            if (!planned.getKey()
                .equals(otherStep.getKey())) {
                return Optional.of(otherStep.getKey());
            }
            if (!planned.getValue()
                .containsAll(otherStep.getValue())
                    || !otherStep.getValue()
                        .containsAll(planned.getValue())) {
                return Optional.of(otherStep.getKey());
            }
        }

        if (itOther.hasNext()) {
            return Optional.of(itOther.next()
                .getKey());
        }

        return Optional.empty();
    }

    /**
     *
     * Check whether this plan and {@code other} share the same history.
     *
     * It might also happen that the entire new plan is already in the history.
     *
     * @param other
     *            new plan without any executed steps.
     * @return true, if the executed steps of this plan are the starting sequence of the new plan.
     *
     */
    public boolean hasCommonHistory(final Plan other) {
        final Iterator<Entry<Double, Set<ScalingPolicy>>> itExecuted = this.executedSteps.entrySet()
            .iterator();

        final Iterator<Entry<Double, Set<ScalingPolicy>>> itOther = new TreeMap<>(other.getPlanSteps()).entrySet()
            .iterator();

        while (itExecuted.hasNext() && itExecuted.next()
            .getKey() < other.getTimeOfNextStep()) {
            // move iterator of history forward to cover missing history of incoming plan.
        }

        while (itOther.hasNext()) {
            if (!itExecuted.hasNext()) {
                return true;
            }

            final Entry<Double, Set<ScalingPolicy>> executedStep = itExecuted.next();
            final Entry<Double, Set<ScalingPolicy>> otherStep = itOther.next();

            if (!executedStep.getKey()
                .equals(otherStep.getKey())) {
                return false; // no common prefix (key)
            }
            if (!executedStep.getValue()
                .containsAll(otherStep.getValue())
                    || !otherStep.getValue()
                        .containsAll(executedStep.getValue())) {
                return false; // no common prefix (value)
            }
        }

        return true;
    }

//    /**
//     * Remove all steps that happen at a point in time greater or equal to {@code time} from this
//     * plan and instead add all steps that happen at a point in time greater or equal to
//     * {@code time} from {@code newPlan}.
//     *
//     * Steps removed by this operation are not moved to the history of executed steps.
//     *
//     * @param newPlan
//     *            the new plan, must not yet be executed
//     * @param time
//     *            all steps with time equal or greater that this value are replaced.
//     */
//    public void updatePlan(final Plan newPlan, final double time) {
//        if (time < this.planSteps.firstKey()) {
//            throw new IllegalArgumentException(String.format("TODO"));
//        }
//
//        this.planSteps.keySet()
//            .removeIf(key -> key >= time); // according to doc, reflects to map.
//
//        final Map<Double, Set<ScalingPolicy>> newSteps = newPlan.getPlanSteps();
//        newSteps.keySet()
//            .removeIf(key -> key < time); // according to doc, reflects to map.
//
//        this.planSteps.putAll(newSteps);
//    }

    /**
     *
     * @return A copy of the planned steps.
     */
    public Map<Double, Set<ScalingPolicy>> getPlanSteps() {
        return Map.copyOf(planSteps);
    }

    /**
     *
     * @return true, if there are no more steps planned.
     */
    public boolean isEmpty() {
        return this.planSteps.isEmpty();
    }

    /**
     *
     * @return point in time of the next step to be executed.
     */
    public double getTimeOfNextStep() {
        return this.planSteps.firstKey();
    }

    /**
     *
     * Forward plan, such that the point in time of the next step is greater than or equal to
     * {@code time}.
     *
     * Moves the passed steps to the history of executed steps.
     *
     * @param time
     */
    public void forwardPlanTo(final Double time) {
        while (!this.isEmpty() && this.planSteps.firstKey() < time) {
            executedSteps.put(this.planSteps.firstKey(), this.planSteps.get(this.planSteps.firstKey()));
            this.planSteps.remove(this.planSteps.firstKey());
        }
    }

    /**
     *
     * @return
     */
    public Set<ModelAdjustmentRequested> executeNextStep() {

        final Entry<Double, Set<ScalingPolicy>> executedStep = this.planSteps.pollFirstEntry();
        this.executedSteps.put(executedStep.getKey(), executedStep.getValue());

        final Set<ModelAdjustmentRequested> events = new HashSet<>();
        for (final ScalingPolicy policy : executedStep.getValue()) {
            events.add(new ModelAdjustmentRequested(policy));
        }

        return events;
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
