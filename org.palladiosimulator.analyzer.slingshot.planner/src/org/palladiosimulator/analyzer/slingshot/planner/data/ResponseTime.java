package org.palladiosimulator.analyzer.slingshot.planner.data;

public class ResponseTime extends Measurement<Double> {
	public ResponseTime(Double responseTime, double timeStamp) {
		super(responseTime, timeStamp);
	}
	
	public Double getResponseTime() {
		return super.getMeasure();
	}
}
