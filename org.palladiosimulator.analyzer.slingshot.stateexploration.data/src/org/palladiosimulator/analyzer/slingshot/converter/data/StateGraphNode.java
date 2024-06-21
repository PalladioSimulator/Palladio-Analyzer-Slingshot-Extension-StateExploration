package org.palladiosimulator.analyzer.slingshot.converter.data;

import java.util.Comparator;
import java.util.List;

import org.palladiosimulator.analyzer.slingshot.converter.data.Utility.UtilityType;
import org.palladiosimulator.spd.ScalingPolicy;

public record StateGraphNode(String id, double startTime, double endTime, List<MeasurementSet> measurements,
		List<SLO> slos, Utility utility, String parentId, ScalingPolicy incomingPolicy) {

	public StateGraphNode(final String id, final double startTime, final double endTime, final List<MeasurementSet> measurements, final List<SLO> slos, final String parentId, final ScalingPolicy incomingPolicy) {
		this(id, startTime, endTime, measurements, slos, calcUtility(startTime, endTime, measurements, slos), parentId, incomingPolicy);
	}

	public double duration() {
		return endTime - startTime;
	}


	/**
	 * This calculates the utility of the state.
	 * In the form of "(slo1 - measure1) + (slo2 - measure2)"
	 * In addition to this, the sum is multiplied with the duration of the state.
	 * This balances shorter against longer paths so they are compatible.
	 * @return
	 */
	private static Utility calcUtility(final double startTime, final double endTime, final List<MeasurementSet> measurements, final List<SLO> slos) {

		final var utility = new Utility();

		for (final SLO slo : slos) {
			final MeasurementSet ms = measurements.stream()
					.filter(x -> x.getSpecificationId().equals(slo.specificationId()))
					.findFirst()
					.orElse(null);
			if (ms != null) {
				final double area = calculateSloUtility(ms, slo.lowerThreshold().doubleValue(),
						slo.upperThreshold().doubleValue());
				utility.addDataInstance(slo.id(), area, UtilityType.SLO);
			}
		}

		for (final var ms : measurements) {
			if (ms.getMonitorName().startsWith("Cost_")) {
				final double area = calculateAreaUnderCurve(ms);
				utility.addDataInstance(ms.getMonitorName(), -area, UtilityType.COST);
			}
		}

		utility.calculateTotalUtility();
		return utility;
	}


	public static double score(final double value, final double lowerThreshold, final double upperThreshold) {
		final double middle = (lowerThreshold + upperThreshold) / 2.0;
		final double range = upperThreshold - lowerThreshold;
		final double sigma = range / 6.0; // Adjust sigma for the desired spread of the normal distribution

		if (value < lowerThreshold) {
			return lowerThreshold - value;
		} else if (value > upperThreshold) {
			return value - upperThreshold;
		} else {
			// Calculate the score using a normal distribution centered at the midpoint
			final double exponent = -Math.pow(value - middle, 2) / (2 * Math.pow(sigma, 2));
			return Math.exp(exponent) * 10;
		}
	}

	private static double calculateSloUtility(final MeasurementSet ms, final double lowerTreshold,
			final double upperTreshold) {
		double area = 0.0;
		final var measurements = ms.getElements();

		// Sort measurements by time to ensure correct integration
		measurements.sort(Comparator.comparingDouble((x) -> x.timeStamp()));

		for (int i = 0; i < measurements.size() - 1; i++) {
			final var current = measurements.get(i);
			final var next = measurements.get(i + 1);

			final double currentValue = current.measure().doubleValue();
			final double nextValue = next.measure().doubleValue();

			area += calculateTrapezoidArea(current.timeStamp(), next.timeStamp(),
					score(currentValue, lowerTreshold, upperTreshold),
					score(nextValue, lowerTreshold, upperTreshold));

		}

		return area;
	}

	private static double calculateAreaUnderCurve(final MeasurementSet ms) {
		double area = 0.0;
		final var measurements = ms.getElements();

		// Sort measurements by time to ensure correct integration
		measurements.sort(Comparator.comparingDouble((x) -> x.timeStamp()));

		for (int i = 0; i < measurements.size() - 1; i++) {
			final var current = measurements.get(i);
			final var next = measurements.get(i + 1);

			final double currentValue = current.measure().doubleValue();
			final double nextValue = next.measure().doubleValue();

			area += calculateTrapezoidArea(current.timeStamp(), next.timeStamp(), currentValue, nextValue);
		}

		return area;
	}

	private static double calculateTrapezoidArea(final double startTime, final double endTime, final double startValue,
			final double endValue) {
		final double width = endTime - startTime;
		final double height = (endValue + startValue);
		return width * height / 2;
	}
}

