package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;

public class JobRecord {

	private final Job job;

	private final double requestedDemand;
	private double normalizedDemand;

	public JobRecord(final Job job) {
		this.job = job;
		this.requestedDemand = job.getDemand();
	}

	public Job getJob() {
		return job;
	}

	public double getRequestedDemand() {
		return requestedDemand;
	}

	public double getCurrentDemand() {
		return job.getDemand();
	}

	public double getNormalizedDemand() {
		return normalizedDemand;
	}

	public void setNormalizedDemand(final double normalizedDemand) {
		this.normalizedDemand = normalizedDemand;
	}

}
