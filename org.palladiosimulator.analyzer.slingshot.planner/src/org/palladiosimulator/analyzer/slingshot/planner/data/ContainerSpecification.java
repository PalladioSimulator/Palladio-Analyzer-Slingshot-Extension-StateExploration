package org.palladiosimulator.analyzer.slingshot.planner.data;

public class ContainerSpecification extends ResourceSpecification {
	private int replicas;
	private double processingRate;
	private String schedulingPolicy;
	private double MTTR;
	private double MTTF;

	public ContainerSpecification(String id, int replicas, double processingRate, String schedulingPolicy, double MTTR, double MTTF) {
		super(id);
		this.replicas = replicas;
		this.processingRate = processingRate;
		this.schedulingPolicy = schedulingPolicy;
		this.MTTR = MTTR;
		this.MTTF = MTTF;
	}
	
	public int getReplicas() {
		return replicas;
	}

	public double getProcessingRate() {
		return processingRate;
	}
	
	public String getSchedulingPolicy() {
		return schedulingPolicy;
	}
	
	public double getMTTR() {
		return MTTR;
	}
	
	public double getMTTF() {
		return MTTF;
	}

}
