package org.palladiosimulator.analyzer.slingshot.converter.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.measure.Measure;

/**
 * Simplified version of ArrayList<Measurment<Double>> with the additional method for median and average.
 *
 * @author Jonas Edlhuber
 *
 */
public class MeasurementSet {

	public record Measurement<T>(T measure, double timeStamp) {

	}

	private final List<Measurement<Number>> elements = new ArrayList<>();

	private Double measurementsSetMedian;
	private Double measurementsSetAverage;

	private String name;
	private String monitorId;
	private String monitorName;
	private String specificationId;
	private String specificationName;
	private String metricName;
	private String metricDescription;
	private String metricDescriptionId;
	

	private List<Measure> measure;

	public MeasurementSet() {
		super();
	}

	public MeasurementSet(final Collection<? extends Measurement<Number>> c) {
		this.elements.addAll(c);
	}

	public List<Measurement<Number>> getElements() {
		return this.elements;
	}

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(final String metricName) {
		this.metricName = metricName;
	}

	public String getMetricDescription() {
		return metricDescription;
	}

	public void setMetricDescription(final String metricDescription) {
		this.metricDescription = metricDescription;
	}

	public String getSpecificationId() {
		return specificationId;
	}

	public void setSpecificationId(final String specificationId) {
		this.specificationId = specificationId;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}


	public String getMonitorId() {
		return monitorId;
	}

	public void setMonitorId(final String monitorId) {
		this.monitorId = monitorId;
	}

	public String getMonitorName() {
		return monitorName;
	}

	public void setMonitorName(final String monitorName) {
		this.monitorName = monitorName;
	}

	public String getMetricDescriptionId() {
		return metricDescriptionId;
	}

	public void setMetricDescriptionId(final String metricDescriptionId) {
		this.metricDescriptionId = metricDescriptionId;
	}

	public double getMedian() {
		if (elements.size() < 1) {
			return 0;
		}
		if (measurementsSetMedian == null) {
			final double[] doubles = elements.stream().mapToDouble(x -> x.measure().doubleValue()).sorted().toArray();
			measurementsSetMedian = doubles[doubles.length/2];
		}
		return measurementsSetMedian;
	}

	public double getAverage() {
		if (elements.size() < 1) {
			return 0;
		}
		if (measurementsSetAverage == null) {
			measurementsSetAverage = elements.stream().mapToDouble(x -> x.measure().doubleValue()).sum()/elements.size();
		}
		return measurementsSetAverage;
	}


	public boolean add(final Measurement<Number> e) {
		resetCalcuationCaches();
		return elements.add(e);
	}


	private void resetCalcuationCaches() {
		this.measurementsSetAverage = null;
		this.measurementsSetMedian = null;
	}

	public String getSpecificationName() {
		return specificationName;
	}

	public void setSpecificationName(final String specificationName) {
		this.specificationName = specificationName;
	}
	
	public void setMeasure(List<Measure> m) {
		this.measure = m;
	}
	

	public List<Measure> obtainMeasure() {
		return measure;
	}
}
