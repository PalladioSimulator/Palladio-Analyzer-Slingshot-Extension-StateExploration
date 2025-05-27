package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;

/**
 *
 * Record for a single Job.
 *
 * The record contains information about a job's demand at various stages of
 * processing. The demands are important to construct a jobs that may recreate a
 * processing resource's state.
 * 
 * This record associates two demands with each {@link Job} instance, a
 * requested demand and a normalised demand. The requested demand is the demand
 * as defined in the PCM models. The normalised demand is the demand, after the
 * requested entered the simulated processing resources, because those resources
 * normalise the demand with their processing rate, c.f
 * {@code org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.resources.active.AbstractActiveResource#onJobInitiated}.
 * 
 * And later on, when actually taking a Snapshot we need both demands, the
 * original and the normalized one, c.f.
 * {@link LessInvasiveInMemoryCamera#handlePFCFSJobs} and
 * {@link LessInvasiveInMemoryCamera#handleProcSharingJobs}
 *
 * @author Sophie Stieß
 *
 */
public class JobRecord {

	private final Job job;

	/** Initial demand, as defined in the model */
	private final double requestedDemand;

	/** Demand, after the Job entered the Resource, normalized with the processing rate of the resource */
	private double normalizedDemand;

	/**
	 * create record and set requested demand.
	 * 
	 * @param job
	 */
	public JobRecord(final Job job) {
		this.job = job;
		this.requestedDemand = job.getDemand();
	}

	public Job getJob() {
		return job;
	}

	/**
	 * @return demand as requested from the processing resource.
	 */
	public double getRequestedDemand() {
		return requestedDemand;
	}

	/**
	 * @return currently unprocessed portion of the normalized demand.
	 */
	public double getCurrentDemand() {
		return job.getDemand();
	}

	/**
	 * @return demand normalized by the resources processing rate.
	 */
	public double getNormalizedDemand() {
		return normalizedDemand;
	}

	public void setNormalizedDemand(final double normalizedDemand) {
		this.normalizedDemand = normalizedDemand;
	}

}
