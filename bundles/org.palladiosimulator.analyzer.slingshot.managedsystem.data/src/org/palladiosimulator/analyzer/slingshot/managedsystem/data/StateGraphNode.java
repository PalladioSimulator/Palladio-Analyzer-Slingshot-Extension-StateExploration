package org.palladiosimulator.analyzer.slingshot.managedsystem.data;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import javax.measure.Measure;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.MeasurementSet.Measurement;
import org.palladiosimulator.servicelevelobjective.LinearFuzzyThreshold;
import org.palladiosimulator.servicelevelobjective.NegativeQuadraticFuzzyThreshold;
import org.palladiosimulator.servicelevelobjective.QuadraticFuzzyThreshold;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.servicelevelobjective.SoftThreshold;
import org.palladiosimulator.servicelevelobjective.Threshold;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 *
 *
 * @author Jonas(?), Raphael Straub, Sophie Stieß
 *
 */
public record StateGraphNode(String id, double startTime, double endTime, List<MeasurementSet> measurements,
        List<SLO> slos, Utility utility, String parentId, List<ScalingPolicy> incomingPolicies) {

    private static final Logger LOGGER = Logger.getLogger(StateGraphNode.class);

    public StateGraphNode(final String id, final double startTime, final double endTime,
            final List<MeasurementSet> measurements, final List<ServiceLevelObjective> slos, final String parentId,
            final List<ScalingPolicy> incomingPolicies) {
        this(id, startTime, endTime, measurements, slos.stream()
            .map(x -> visitServiceLevelObjective(x))
            .toList(),
                calcUtility(startTime, endTime, measurements, slos), parentId, incomingPolicies);
    }

    public static SLO visitServiceLevelObjective(final ServiceLevelObjective slo) {
        return new SLO(slo.getId(), slo.getName(), slo.getMeasurementSpecification()
            .getId(),
                (Number) slo.getLowerThreshold()
                    .getThresholdLimit()
                    .getValue(),
                (Number) slo.getUpperThreshold()
                    .getThresholdLimit()
                    .getValue());
    }

    private static Utility calcUtility(final double startTime, final double endTime,
            final List<MeasurementSet> measurements,
            final List<ServiceLevelObjective> slos) {
        final var utility = new Utility();

        for (final ServiceLevelObjective slo : slos) {
            final MeasurementSet ms = measurements.stream()
                .filter(x -> x.getSpecificationId()
                    .equals(slo.getMeasurementSpecification()
                        .getId()))
                .findFirst()
                .orElse(null);

            if (ms != null) {
                // Is this correct?
                final var points = IntStream.range(0, ms.getElements()
                    .size())
                    .mapToObj(x -> {
                        return new Measurement<Number>(getGrade(ms.obtainMeasure()
                            .get(x), slo.getLowerThreshold(), slo.getUpperThreshold()), ms.getElements()
                                .get(x)
                                .timeStamp());
                    })
                    .toList();
                utility.addDataInstance(slo.getId(), points, Utility.UtilityType.SLO);
            }
        }

        for (final var ms : measurements) {
            if (ms.getMonitorName()
                .startsWith("Cost_")) {
                utility.addDataInstance(ms.getMonitorName(), ms.getElements(), Utility.UtilityType.COST);
            }
        }

        utility.calculateTotalUtility();
        return utility;
    }

    public double duration() {
        return endTime - startTime;
    }

    /**
     * Computes the grade of fulfillment of a measurement regarding the lower and upper threshold
     *
     * Taken from:
     * https://github.com/PalladioSimulator/Palladio-Addons-ServiceLevelObjectives/blob/master/bundles/org.palladiosimulator.servicelevelobjective.edp2/src/org/palladiosimulator/servicelevelobjective/edp2/mappers/SLOViolationEDP2DatasourceMapper.java
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static double getGrade(final Measure measurement, final Threshold lower, final Threshold upper) {

        if (lower != null) {
            final Measure lowerThresholdHardLimit = lower.getThresholdLimit();
            if (measurement.compareTo(lowerThresholdHardLimit) < 0) {
                return 0.0;
            }
            if ((lower instanceof SoftThreshold)) {
                final Measure lowerThresholdSoftLimit = ((SoftThreshold) lower).getSoftLimit();
                if (measurement.compareTo(lowerThresholdSoftLimit) < 0) {
                    return gradeSoftLowerThreshold(measurement, (SoftThreshold) lower);
                }
            }
        }
        if (upper != null) {
            final Measure upperThresholdHardLimit = upper.getThresholdLimit();
            if ((upper instanceof SoftThreshold)) {
                final Measure upperThresholdSoftLimit = ((SoftThreshold) upper).getSoftLimit();
                if (measurement.compareTo(upperThresholdSoftLimit) <= 0) {
                    return 1.0;
                } else if (measurement.compareTo(upperThresholdHardLimit) <= 0) {
                    return gradeSoftUpperThreshold(measurement, (SoftThreshold) upper);
                }
            } else if (measurement.compareTo(upperThresholdHardLimit) <= 0) {
                return 1.0;
            }
        } else {
            return 1.0;
        }

        return 0.0;
    }

    /**
     * Handles grading of measurements in lower fuzzy range
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static double gradeSoftLowerThreshold(final Measure toGrade, final SoftThreshold lowerThreshold) {
        final double x = (double) toGrade.getValue();
        final double soft = lowerThreshold.getSoftLimit()
            .doubleValue(toGrade.getUnit());
        final double hard = lowerThreshold.getThresholdLimit()
            .doubleValue(toGrade.getUnit());

        if (lowerThreshold instanceof LinearFuzzyThreshold) {
            return 1 / (soft - hard) * (x - hard);
        }
        if (lowerThreshold instanceof QuadraticFuzzyThreshold) {
            return 1 / Math.pow((soft - hard), 2) * Math.pow((x - hard), 2);
        }
        if (lowerThreshold instanceof NegativeQuadraticFuzzyThreshold) {
            return 1 - (1 / Math.pow((soft - hard), 2) * Math.pow((x - soft), 2));
        }
        return 0;
    }

    /**
     * Handles grading of measurements in upper fuzzy range
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static double gradeSoftUpperThreshold(final Measure toGrade, final SoftThreshold upperThreshold) {
        final double x = (double) toGrade.getValue();
        final double soft = upperThreshold.getSoftLimit()
            .doubleValue(toGrade.getUnit());
        final double hard = upperThreshold.getThresholdLimit()
            .doubleValue(toGrade.getUnit());
        if (upperThreshold instanceof LinearFuzzyThreshold) {
            return -1 / (hard - soft) * (x - hard);
        }
        if (upperThreshold instanceof QuadraticFuzzyThreshold) {
            return 1 / Math.pow((hard - soft), 2) * Math.pow((x - hard), 2);
        }
        if (upperThreshold instanceof NegativeQuadraticFuzzyThreshold) {
            return 1 - (1 / Math.pow((hard - soft), 2) * Math.pow((x - soft), 2));
        }
        return 0;
    }

    /**
     * Calculates the area under a curve represented by a set of measurements.
     *
     * @param ms
     *            the set of measurements representing the curve
     * @return the approximate area under the curve
     */
    private static double calculateAreaUnderCurve(final List<Measurement<Number>> measurements) {
        double area = 0.0;

        // Iterate through the measurements pairwise to calculate the area under each
        // segment
        for (int i = 0; i < measurements.size() - 1; i++) {
            // Current and next measurement points
            final var current = measurements.get(i);
            final var next = measurements.get(i + 1);

            // The values (heights) at the current and next time stamps
            final double currentValue = current.measure()
                .doubleValue();
            final double nextValue = next.measure()
                .doubleValue();

            // The time stamps (positions along the x-axis) for the current and next
            // measurements
            final double start = current.timeStamp(); // x₁
            final double end = next.timeStamp(); // x₂

            // Calculate the area under the segment between the two points
            area += calculateArea(start, end, currentValue, nextValue);
        }

        return area;
    }

    /**
     *
     * Maps measurements to a curve of SLO grades and calculate the area under a grades curve.
     *
     * E.g. for measurements with time-value tuples [(0,2.5), (1,2.6), (2,4)] and an SLO with upper
     * threshold t_soft = 3 and t_hard = 3.5, the graded curve is [(0,1), (1,1), (2,0)] and the area
     * underneath is 1 + 0.5 = 1.5.
     *
     *
     *
     * @param measurements
     *            measurements to be graded
     * @param startTime
     * @param slo
     *            SLOs to grade against
     * @return area under graded curve.
     */
    private static double calculateAreaUnderCurveMeasure(final MeasurementSet measurements,
            final ServiceLevelObjective slo) {
        double area = 0.0;

        final List<Measurement<Number>> points = IntStream.range(0, measurements.getElements()
            .size())
            .mapToObj(x -> {
                return new Measurement<Number>(
                        getGrade(measurements.obtainMeasure()
                            .get(x), slo.getLowerThreshold(), slo.getUpperThreshold()),
                        measurements.getElements()
                            .get(x)
                            .timeStamp());
            })
            .sorted(Comparator.comparingDouble((x) -> x.timeStamp()))
            .toList();

        // Iterate through the measurements pairwise to calculate the area under each
        // segment
        for (int i = 0; i < points.size() - 1; i++) {
            // Current and next measurement points
            final var current = points.get(i);
            final var next = points.get(i + 1);

            // The values (heights) at the current and next time stamps
            final double currentValue = current.measure()
                .doubleValue();
            final double nextValue = next.measure()
                .doubleValue();

            // The time stamps (positions along the x-axis) for the current and next
            // measurements
            final double start = current.timeStamp(); // x₁
            final double end = next.timeStamp(); // x₂

            // Calculate the area under the segment between the two points
            area += calculateArea(start, end, currentValue, nextValue);
        }

        return area;
    }

    /**
     * Calculates the area of a trapezoid defined by two points on a curve.
     *
     * @param start
     *            the starting time stamp (x₁)
     * @param end
     *            the ending time stamp (x₂)
     * @param currentValue
     *            the value at the starting time stamp (f(x₁))
     * @param nextValue
     *            the value at the ending time stamp (f(x₂))
     * @return the area under the curve segment between start and end
     */
    private static double calculateArea(final double start, final double end, final double currentValue,
            final double nextValue) {
        // Calculate the width of the interval (the distance along the x-axis)
        final double timeDifference = end - start; // Δx = x₂ - x₁

        // Calculate the average of the two values (heights at x₁ and x₂)
        final double averageValue = (currentValue + nextValue) / 2.0; // (f(x₁) + f(x₂)) / 2

        // The area of the trapezoid is the product of the average height and the width
        final double area = averageValue * timeDifference; // Area = ((f(x₁) + f(x₂)) / 2) * Δx

        return area;
    }
}
