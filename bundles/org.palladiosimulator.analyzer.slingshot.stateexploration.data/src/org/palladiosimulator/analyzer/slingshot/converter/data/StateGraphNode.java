package org.palladiosimulator.analyzer.slingshot.converter.data;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.converter.data.MeasurementSet.Measurement;
import org.palladiosimulator.analyzer.slingshot.converter.data.Utility.UtilityType;
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

	public StateGraphNode(final String id, final double startTime, final double endTime,
			final List<MeasurementSet> measurements, final List<SLO> slos, final String parentId,
			final List<ScalingPolicy> incomingPolicies) {
		this(id, startTime, endTime, measurements, slos, calcUtility(startTime, endTime, measurements, slos), parentId,
				incomingPolicies);
	}

	public double duration() {
		return endTime - startTime;
	}


	/**
	 * This calculates the utility of the state. In the form of "(slo1 - measure1) +
	 * (slo2 - measure2)" In addition to this, the sum is multiplied with the
	 * duration of the state. This balances shorter against longer paths so they are
	 * compatible.
	 *
	 * @return
	 */
	private static Utility calcUtility(final double startTime, final double endTime,
			final List<MeasurementSet> measurements, final List<SLO> slos) {

		final var utility = new Utility();

		for (final SLO slo : slos) {
			final MeasurementSet ms = measurements.stream()
					.filter(x -> x.getSpecificationId().equals(slo.specificationId()))
					.findFirst()
					.orElse(null);
			if (ms != null) {
				final var points = ms.getElements().stream()
						// mirror the function at the upper threshold and then subtract the threshold
						// (basically use threshold as new x axis)
						.map(x -> new Measurement<Number>(
								slo.upperThreshold().doubleValue() - x.measure().doubleValue(), x.timeStamp()))
						.sorted(Comparator.comparingDouble((x) -> x.timeStamp()))
						.toList();

				final double area = calculateAreaUnderCurve(points);
				utility.addDataInstance(slo.id(), area, UtilityType.SLO);
			}
		}

		for (final var ms : measurements) {
			if (ms.getMonitorName().startsWith("Cost_")) {
				final double area = calculateAreaUnderCurve(
						ms.getElements().stream().sorted(Comparator.comparingDouble((x) -> x.timeStamp())).toList());
				utility.addDataInstance(ms.getMonitorName(), -area, UtilityType.COST);
			}
		}

		utility.calculateTotalUtility();
		return utility;
	}


	/**
	 * Calculates the area under a curve represented by a set of measurements.
	 *
	 * @param ms the set of measurements representing the curve
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
			final double currentValue = current.measure().doubleValue();
			final double nextValue = next.measure().doubleValue();

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
	 * @param start        the starting time stamp (x₁)
	 * @param end          the ending time stamp (x₂)
	 * @param currentValue the value at the starting time stamp (f(x₁))
	 * @param nextValue    the value at the ending time stamp (f(x₂))
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

