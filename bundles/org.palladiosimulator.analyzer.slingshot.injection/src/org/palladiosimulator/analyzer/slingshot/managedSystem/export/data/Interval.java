package org.palladiosimulator.analyzer.slingshot.managedSystem.export.data;

/**
 *
 * Simple interval consisting of a lower and an upper bound. Neither bound must be negative and the
 * upper bound must not be smaller the the lower bound. The might be equal though.
 *
 * @note i probabaly must un-record this later on, or i cannot use it for deserialising the json.
 *
 * @author Sarah Stieß
 *
 */
public record Interval(double lowerBound, double upperBound) {
    public Interval {
        if (lowerBound < 0) {
            throw new IllegalArgumentException("lower bound must not be negative, but is.");
        }
        if (upperBound < 0) {
            throw new IllegalArgumentException("upper bound must not be samller than lower bound, but is.");
        }
    }
}
