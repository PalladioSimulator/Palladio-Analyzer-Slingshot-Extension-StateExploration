package org.palladiosimulator.analyzer.slingshot.converter.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.converter.data.MeasurementSet.Measurement;

public class Utility {
	record UtilityData(String id, double utility, UtilityType type) {
	}

	enum UtilityType {
		SLO, COST;
	}

	private static final double STEP_SIZE = 1.0;
	private static final Logger LOGGER = Logger.getLogger(Utility.class);
	private double totalUtility;
	private final List<UtilityData> data = new ArrayList<>();;

	private final List<List<Measurement<Double>>> slo = new ArrayList<>();
	private final List<List<Measurement<Double>>> costs = new ArrayList<>();

	public void addDataInstance(final String id, final List<Measurement<Number>> measurements, final UtilityType type) {
		data.add(new UtilityData(id,
				// Using sum to keep compatibility with backend
				measurements.stream().mapToDouble(x -> x.measure().doubleValue()).sum(), type));
		switch (type) {
		case SLO:
			slo.add(measurements.stream().map(x -> (Measurement<Double>) ((Measurement) x)).toList());
			break;
		case COST:
			costs.add(measurements.stream().map(x -> (Measurement<Double>) ((Measurement) x)).toList());
			break;
		}
	}

	public void calculateTotalUtility() {

		// System.out.println("SLO TIMESTAPS:");
		// System.out.println("MIN:" + slo.stream().mapToDouble(
		// x -> x.stream().mapToDouble(y -> { return y.timeStamp();}).min().orElse(-1)
		// ).filter(x -> x >= -0.5)
		// .min()
		// .orElse(-1));
		// System.out.println("MAX:" + slo.stream().mapToDouble(
		// x -> x.stream().mapToDouble(y -> { return y.timeStamp();}).max().orElse(-1)
		// ).max()
		// .orElse(-1));
		// System.out.println("ALL:" + slo.stream().map(
		// x -> {return x.stream().map(y -> { return
		// y.timeStamp();}).toList();}).toList());

		// System.out.println("\n\nCOST TIMESTAPS:");
		// System.out.println("MIN:" + costs.stream().mapToDouble(
		// x -> x.stream().mapToDouble(y -> { return y.timeStamp();}).min().orElse(-1)
		// ).filter(x -> x >= -0.5)
		// .min()
		// .orElse(-1));
		// System.out.println("MAX:" + costs.stream().mapToDouble(
		// x -> x.stream().mapToDouble(y -> { return y.timeStamp();}).max().orElse(-1)
		// ).max()
		// .orElse(-1));
		// System.out.println("ALL:" + costs.stream().map(
		// x -> {return x.stream().map(y -> { return
		// y.timeStamp();}).toList();}).toList());

		final var maybeMin = List.of(slo.stream().mapToDouble(x -> x.stream().mapToDouble(y -> {
			return y.timeStamp();
		}).min().orElse(-1)).filter(x -> x >= -0.5).min().orElse(-1),
				costs.stream().mapToDouble(x -> x.stream().mapToDouble(y -> {
					return y.timeStamp();
				}).max().orElse(-1)).filter(x -> x >= -0.5).min().orElse(-1)).stream().mapToDouble(x -> x)
				.filter(x -> x >= -0.5).min();

		// When maybeMin is empty then no data series has any measurements
		if(maybeMin.isEmpty()) {
			totalUtility = 0;
			return;
		}

		final double min = (int) maybeMin.getAsDouble();
		final var max = (int) List.of(slo.stream().mapToDouble(x -> x.stream().mapToDouble(y -> {
			return y.timeStamp();
		}).max().orElse(-1)).max().orElse(-1), costs.stream().mapToDouble(x -> x.stream().mapToDouble(y -> {
			return y.timeStamp();
		}).max().orElse(-1)).max().orElse(-1)).stream().mapToDouble(x -> x).max().getAsDouble();

		// System.out.println("Total:");
		// System.out.println("Min: " + min);
		// System.out.println("Max: " + max);

		final List<List<Measurement<Double>>> sloIntervals = computeIntervals(min, max, slo);
		final List<List<Measurement<Double>>> costIntervals = computeIntervals(min, max, costs);

		// ensure that the length of all list is equal
		if(sloIntervals.stream().map(x -> {
			return x.size();
		}).distinct().count() > 1 || costIntervals.stream().map(x -> {
			return x.size();
		}).distinct().count() > 1) {
			throw new IllegalStateException("Measurements differ in length. \nSLO (" + sloIntervals.stream().map(x -> {
				return x.size();
			}).distinct().toList() + "):\n" + sloIntervals + "\nCost (" + costIntervals.stream().map(x -> {
				return x.size();
			}).distinct().toList() + "):\n" + costIntervals);
		}

		final List<Measurement<Double>> toIntegrate = new ArrayList<Measurement<Double>>();
		final var length = sloIntervals.get(0).size();

		for(int i = 0; i < length; i++) {
			final int a = i;
			final var slo = sloIntervals.stream().mapToDouble(x -> x.get(a).measure()).sum();
			final var cost = costIntervals.stream().mapToDouble(x -> x.get(a).measure()).sum();
			// ensure equality of all timestamps
			final var timeStampMax = List
					.of(costIntervals.stream().mapToDouble(x -> x.get(a).timeStamp()).max(),
							sloIntervals.stream().mapToDouble(x -> x.get(a).timeStamp()).max())
					.stream().mapToDouble(x -> x.orElse(0.0)).max().orElse(1.0);
			final var timeStampMin = List
					.of(costIntervals.stream().mapToDouble(x -> x.get(a).timeStamp()).min(),
							sloIntervals.stream().mapToDouble(x -> x.get(a).timeStamp()).min())
					.stream().mapToDouble(x -> x.orElse(0.0)).min().orElse(0.0);
			if(timeStampMax != timeStampMin) {
				throw new IllegalStateException("Measurements have different timestamps at index " + i + ". SLO:\n"
						+ sloIntervals.get(i) + "\nCost:\n" + costIntervals.get(i));
			}

			toIntegrate.add(new Measurement<Double>(slo / cost, timeStampMin));
		}

		totalUtility = calculateAreaUnderCurve(toIntegrate);

		// cleanup for serialization
		slo.clear();
		costs.clear();
	}

	private static List<List<Measurement<Double>>> computeIntervals(final double start, final double end,
			final List<List<Measurement<Double>>> list) {
		return list.stream().map(x -> computeIntervalList(start, end, new ArrayList<>(x))).toList();

	}

	private static List<Measurement<Double>> computeIntervalList(final double start, final double end,
			final List<Measurement<Double>> list) {
		final var result = new ArrayList<Measurement<Double>>();

		// System.out.println("computeIntervalList");

		final var iterator = list.listIterator();

		if (iterator.hasNext() == false) {
			// System.out.println("iterator empty fill with 0");
			fillCarry(start, end, result, 0.0);
		} else {
			var carry = 0.0;
			for (double s = start; s <= end; s += STEP_SIZE) {
				if (iterator.hasNext() == false) {
					// System.out.println("iterator got empty, fill with carry");
					fillCarry(s, end, result, carry);
					break;
				}
				final var current = iterator.next();

				// System.out.println("Filling carry at (" + s + ") until timestamp: " +
				// current.timeStamp());
				for (; s + STEP_SIZE <= current.timeStamp(); s += STEP_SIZE) {
					result.add(new Measurement<Double>(carry, s));
				}
				// System.out.println("Stopping carry fill at (" + s + ")");
				// when we reach this point, current.timeStamp() is between s and s+STEP_SIZE
				// now we aggregate all measurements in this interval
				// System.out.println("computing new carry");
				final var measurements = new ArrayList<Double>();
				measurements.add(current.measure());
				while(iterator.hasNext()) {
					final var next = iterator.next();
					if (next.timeStamp() < s + STEP_SIZE) {
						measurements.add(next.measure());
					} else {
						// set iterator back
						iterator.previous();
						break;
					}
				}
				carry = measurements.stream().mapToDouble(x -> x).average().orElse(0.0);
				// System.out.println("New carry: " + carry);
				result.add(new Measurement<Double>(carry, s));
			}

		}

		// System.out.println("Computed intervals with length" + result.size());

		return result;
	}

	private static void fillCarry(final double start, final double end, final List<Measurement<Double>> list,
			final double carry) {
		// System.out.println("Beginn fill at: " + list.size());
		for(double s = start; s <= end; s += STEP_SIZE) {
			list.add(new Measurement<Double>(carry, s));
		}
		// System.out.println("End fill at: " + list.size());
	}

	/**
	 * Calculates the area under a curve represented by a set of measurements.
	 *
	 * @param ms the set of measurements representing the curve
	 * @return the approximate area under the curve
	 */
	private static double calculateAreaUnderCurve(final List<Measurement<Double>> toIntegrate) {
		double area = 0.0;

		// Iterate through the measurements pairwise to calculate the area under each
		// segment
		for(int i = 0; i < toIntegrate.size() - 1; i++) {
			// Current and next measurement points
			final var current = toIntegrate.get(i);
			final var next = toIntegrate.get(i + 1);

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

	public double getTotalUtilty() {
		return totalUtility;
	}

}
