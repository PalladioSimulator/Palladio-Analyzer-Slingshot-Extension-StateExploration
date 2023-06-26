package org.palladiosimulator.analyzer.slingshot.planner.data;


public class Measurement<T> {
	private T measure;
	private double timeStamp;
	
	public Measurement(T measure, double timeStamp) {
		this.measure = measure;
		this.timeStamp = timeStamp;
	}

	public T getMeasure() {
		return measure;
	}
	
	public double getTimeStamp() {
		return timeStamp;
	}
}
