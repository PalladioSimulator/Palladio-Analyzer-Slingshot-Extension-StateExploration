package org.palladiosimulator.analyzer.slingshot.planner.data;

public class HDDContainerSpecification extends ContainerSpecification {
	private double writeProcessingRate;
	private double readProcessingRate;

	public HDDContainerSpecification(String id, int replicas, double processingRate, String schedulingPolicy, double MTTR,
			double MTTF, double writeProcessingRate, double readProcessingRate) {
		super(id, replicas, processingRate, schedulingPolicy, MTTR, MTTF);
		this.writeProcessingRate = writeProcessingRate;
		this.readProcessingRate = readProcessingRate;
	}

	public double getWriteProcessingRate() {
		return writeProcessingRate;
	}
	
	public double getReadProcessingRate() {
		return readProcessingRate;
	}
}
