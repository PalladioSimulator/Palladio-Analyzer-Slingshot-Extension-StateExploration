package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.User;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;
import org.palladiosimulator.analyzer.slingshot.common.utils.events.ModelPassedEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.EventRecord;
import org.palladiosimulator.pcm.resourceenvironment.ProcessingResourceSpecification;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;

/**
 *
 * @author stiesssh
 *
 */
public class LessInvasiveInMemoryRecord implements EventRecord {
	private static final Logger LOGGER = Logger.getLogger(LessInvasiveInMemoryRecord.class);

	private static final String FCFS_ID = "FCFS";
	private static final String PROCSHARING_ID = "ProcessorSharing";

	public LessInvasiveInMemoryRecord() {
		this.openCalculators = new HashMap<>();
		this.openJob = new HashMap<>();
	}

	/* states that still change due to running simulation */
	private final Map<User, ArrayDeque<ModelPassedEvent<?>>> openCalculators;
	private final Map<User, JobRecord> openJob;

	@Override
	public void addInitiatedCalculator(final UsageModelPassedElement<Start> event) {
		final User user = event.getContext().getUser();
		if (!openCalculators.containsKey(user)) {
			openCalculators.put(user, new ArrayDeque<>());
		}
		assert !openCalculators.get(user).contains(event);

		openCalculators.get(user).push(event);
	}

	@Override
	public void removeFinishedCalculator(final UsageModelPassedElement<Stop> event) {
		final User user = event.getContext().getUser();
		if (openCalculators.containsKey(user)) {
			openCalculators.get(user).pop();
		}
	}

	@Override
	public void removeJobRecord(final JobFinished event) {
		if (event.getEntity() instanceof final ActiveJob job) {
			openJob.remove(job.getRequest().getUser());
		}
	}

	@Override
	public void createJobRecord(final JobInitiated event) {

		if (event.getEntity() instanceof final ActiveJob job) {
			if (openJob.containsKey(job.getRequest().getUser())) {
			throw new IllegalArgumentException(String.format("Cannot create Record for %s, a Record already exsits.", event.getEntity().toString()));
		}

		openJob.put(job.getRequest().getUser(), new JobRecord(job));
	}
	}
	@Override
	public void updateJobRecord(final JobInitiated event) {

		if (event.getEntity() instanceof final ActiveJob job) {
			if (!openJob.containsKey(job.getRequest().getUser())) {
			throw new IllegalArgumentException(String.format("Cannot update %s, missing Record.", event.getEntity().toString()));
		}

		openJob.get(job.getRequest().getUser()).setNormalizedDemand(job.getDemand());
	}
	}

	@Override
	public Set<AbstractEntityChangedEvent<?>> getRecordedCalculators() {
		final Set<AbstractEntityChangedEvent<?>> rval = new HashSet<>();
		openCalculators.values().stream().forEach(adq -> rval.addAll(adq));
		return rval;
	}

	@Override
	public Set<JobRecord> getFCFSJobRecords() {
		return Set.copyOf(
				openJob.values().stream().filter(record -> this.isFCFS(record.getJob())).collect(Collectors.toSet()));
	}

	@Override
	public Set<JobRecord> getProcSharingJobRecords() {
		return Set.copyOf(openJob.values().stream().filter(record -> this.isProcSharing(record.getJob()))
				.collect(Collectors.toSet()));
	}

	/**
	 *
	 * @param job
	 * @return true iff job is processed by an FCFS processing recource.
	 */
	private boolean isFCFS(final ActiveJob job) {
		final Optional<ProcessingResourceSpecification> optSpec = job.getAllocationContext()
				.getResourceContainer_AllocationContext().getActiveResourceSpecifications_ResourceContainer().stream()
				.filter(spec -> spec instanceof ProcessingResourceSpecification).map(spec -> spec).findFirst();

		if (optSpec.isEmpty()) {
			LOGGER.debug(String.format("not a processing resource"));
			return false;
		}
		return optSpec.get().getSchedulingPolicy().getId().equals(FCFS_ID);
	}

	/**
	 *
	 * @param job
	 * @return true iff job is processed by an processor sharing processing resource
	 */
	private boolean isProcSharing(final ActiveJob job) {
		final Optional<ProcessingResourceSpecification> optSpec = job.getAllocationContext()
				.getResourceContainer_AllocationContext().getActiveResourceSpecifications_ResourceContainer().stream()
				.filter(spec -> spec instanceof ProcessingResourceSpecification).map(spec -> spec).findFirst();

		if (optSpec.isEmpty()) {
			LOGGER.debug(String.format("not a processing resource"));
			return false;
		}
		return optSpec.get().getSchedulingPolicy().getId().equals(PROCSHARING_ID);
	}
}
