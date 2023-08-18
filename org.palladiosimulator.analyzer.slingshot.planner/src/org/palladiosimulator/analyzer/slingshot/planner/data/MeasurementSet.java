package org.palladiosimulator.analyzer.slingshot.planner.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Simplified version of ArrayList<Measurment<Double>> with the additional method for median and average.
 * 
 * @author Jonas Edlhuber
 *
 */
public class MeasurementSet extends ArrayList<Measurement<Double>> {
	private Double measurementsSetMedian;
	private Double measurementsSetAverage;
	
	private String name;
	private String measuringPointURI;
	
	public MeasurementSet() {
		super();
	}

	public MeasurementSet(Collection<? extends Measurement<Double>> c) {
		super(c);
	}

	public MeasurementSet(int initialCapacity) {
		super(initialCapacity);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMeasuringPointURI() {
		return measuringPointURI;
	}
	
	public void setMeasuringPointURI(String uri) {
		this.measuringPointURI = uri;
	}
	
	public double getMedian() {
		if (this.size() < 1) {
			return 0;
		}
		if (measurementsSetMedian == null) {
			double[] doubles = this.stream().mapToDouble(x -> x.getMeasure()).sorted().toArray();
			measurementsSetMedian = doubles[doubles.length/2];
		}
		return measurementsSetMedian;
	}
	
	public double getAverage() {
		if (this.size() < 1) {
			return 0;
		}
		if (measurementsSetAverage == null) {
			measurementsSetAverage = this.stream().mapToDouble(x -> x.getMeasure()).sum()/this.size();
		}
		return measurementsSetAverage;
	}
	
	/**
	 * The following functions are overwritten so after they have been modifying the list the calculated caches are recalculated!
	 */
	
	@Override
	public void add(int index, Measurement<Double> element) {
		super.add(index, element);
		this.measurementsSetAverage = null;
	}

	@Override
	public boolean add(Measurement<Double> e) {
		resetCalcuationCaches();
		return super.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends Measurement<Double>> c) {
		resetCalcuationCaches();
		return super.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends Measurement<Double>> c) {
		resetCalcuationCaches();
		return super.addAll(index, c);
	}

	@Override
	public void clear() {
		super.clear();
		resetCalcuationCaches();
	}

	@Override
	public Measurement<Double> remove(int index) {
		resetCalcuationCaches();
		return super.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		resetCalcuationCaches();
		return super.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		resetCalcuationCaches();
		return super.removeAll(c);
	}

	@Override
	public boolean removeIf(Predicate<? super Measurement<Double>> filter) {
		resetCalcuationCaches();
		return super.removeIf(filter);
	}

	@Override
	protected void removeRange(int fromIndex, int toIndex) {
		super.removeRange(fromIndex, toIndex);
		resetCalcuationCaches();
	}

	@Override
	public void replaceAll(UnaryOperator<Measurement<Double>> operator) {
		super.replaceAll(operator);
		resetCalcuationCaches();
	}

	@Override
	public Measurement<Double> set(int index, Measurement<Double> element) {
		resetCalcuationCaches();
		return super.set(index, element);
	}

	@Override
	public void trimToSize() {
		super.trimToSize();
		resetCalcuationCaches();
	}
	
	/**
	 * Private method to reset the calculation caches.
	 */
	private void resetCalcuationCaches() {
		this.measurementsSetAverage = null;
		this.measurementsSetMedian = null;
	}
}
