package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
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

public class LessInvasiveInMemoryRecord implements EventRecord {
	private static final Logger LOGGER = Logger.getLogger(LessInvasiveInMemoryRecord.class);
	private static final String FAKE = "fakeID";

	public LessInvasiveInMemoryRecord() {
		this.openCalculators = new HashMap<>();
		this.openJob = new HashMap<>();
	}

	/* states that still change due to running simulation */
	private final Map<User, ArrayDeque<ModelPassedEvent<?>>> openCalculators;
	private final Map<User, JobRecord> openJob;

	public void addInitiatedCalculator(final UsageModelPassedElement<Start> event) {
		final User user = event.getContext().getUser();
		if (!openCalculators.containsKey(user)) {
			openCalculators.put(user, new ArrayDeque<>());
		}
		assert !openCalculators.get(user).contains(event);

		openCalculators.get(user).push(event);
	}

	public void removeFinishedCalculator(final UsageModelPassedElement<Stop> event) {
		final User user = event.getContext().getUser();
		if (openCalculators.containsKey(user)) {
			openCalculators.get(user).pop();
		}
	}

	public void clear() {
		this.openCalculators.clear();
		this.openJob.clear();
	}

	@Override
	public void updateRecord(final AbstractEntityChangedEvent<?> event) {

		if (event instanceof JobInitiated) {
			final JobInitiated jobevent = (JobInitiated) event;
			if (jobevent.getEntity().getId().equals(FAKE)) {
				return;
			}

			final User user = jobevent.getEntity().getRequest().getUser();
			if (openJob.containsKey(user)) {
				// on PostIntercept
				openJob.get(jobevent.getEntity().getRequest().getUser())
						.setNormalizedDemand(jobevent.getEntity().getDemand());
			} else {
				// on PreIntercept
				openJob.put(user, new JobRecord(jobevent.getEntity()));
			}

		} else if (event instanceof JobFinished) {
			openJob.remove(((JobFinished) event).getEntity().getRequest().getUser());
		} else if (event instanceof UsageModelPassedElement<?>) {
			final Object modelElement = ((UsageModelPassedElement<?>) event).getModelElement();

			if (modelElement instanceof Start) {
				this.addInitiatedCalculator((UsageModelPassedElement<Start>) event);
			} else if (modelElement instanceof Stop) {
				this.removeFinishedCalculator((UsageModelPassedElement<Stop>) event);
			}
		}
	}

	public Set<AbstractEntityChangedEvent<?>> getRecordedCalculators() {
		final Set<AbstractEntityChangedEvent<?>> rval = new HashSet<>();
		openCalculators.values().stream().forEach(adq -> rval.addAll(adq));
		return rval;
	}

	public Set<JobRecord> getFCFSJobRecords() {
		return Set.copyOf(openJob.values().stream().filter(record -> this.isFCFS(record.getJob())).collect(Collectors.toSet()));
	}
	public Set<JobRecord> getProcSharingJobRecords() {
		return Set.copyOf(openJob.values().stream().filter(record -> this.isProcSharing(record.getJob())).collect(Collectors.toSet()));
	}

	public Job getUpdateJob(final Job job) {
		return Job.builder().withAllocationContext(job.getAllocationContext()).withDemand(0).withId(FAKE)
				.withProcessingResourceType(job.getProcessingResourceType()).build();
	}

	@Override
	public Set<AbstractEntityChangedEvent<?>> getRecord() {
		throw new UnsupportedOperationException("no complete get record for now");
	}

	/**
	 *
	 * @param job
	 * @return true iff job is processed by an FCFS processing recource.
	 */
	private boolean isFCFS(final Job job) {
		final Optional<ProcessingResourceSpecification> optSpec = job.getAllocationContext().getResourceContainer_AllocationContext().getActiveResourceSpecifications_ResourceContainer().stream()
			.filter(spec -> spec instanceof ProcessingResourceSpecification)
			.map(spec -> spec).findFirst();

		if (optSpec.isEmpty()) {
			LOGGER.debug(String.format("not a processing resource"));
			return false;
		}
		return optSpec.get().getSchedulingPolicy().getId().equals("FCFS");
	}

	/**
	 *
	 * @param job
	 * @return true iff job is processed by an processor sharing processing resource
	 */
	private boolean isProcSharing(final Job job) {
		final Optional<ProcessingResourceSpecification> optSpec = job.getAllocationContext().getResourceContainer_AllocationContext().getActiveResourceSpecifications_ResourceContainer().stream()
			.filter(spec -> spec instanceof ProcessingResourceSpecification)
			.map(spec -> spec).findFirst();

		if (optSpec.isEmpty()) {
			LOGGER.debug(String.format("not a processing resource"));
			return false;
		}
		return optSpec.get().getSchedulingPolicy().getId().equals("ProcessorSharing");
	}
}
