package org.palladiosimulator.analyzer.slingshot.planner.data;

public class LinkSpecification extends ResourceSpecification {
	private double latency;
	private int throughput;
	private double failureProbability;

	public LinkSpecification(String id, double latency, int throughput, double failureProbability) {
		super(id);
		this.latency = latency;
		this.throughput = throughput;
		this.failureProbability = failureProbability;
	}
	
	public double getLatency() {
		return latency;
	}
	
	public int getThroughput() {
		return throughput;
	}
	
	public double getFailureProbability() {
		return failureProbability;
	}
}
