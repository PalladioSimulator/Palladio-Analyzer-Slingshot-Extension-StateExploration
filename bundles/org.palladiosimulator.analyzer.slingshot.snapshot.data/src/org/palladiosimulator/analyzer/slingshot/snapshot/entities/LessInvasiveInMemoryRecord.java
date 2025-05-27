package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.LinkingJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.User;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.common.utils.events.ModelPassedEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.EventRecord;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.resourceenvironment.ProcessingResourceSpecification;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.StopAction;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;

/**
 *
 * Implementation of {@link EventRecord} that records events in inmemory data structures. 
 *
 * @author Sarah Stieß
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
	private final Map<User, Map<EObject, ModelPassedEvent<?>>> openCalculators;
	private final Map<User, JobRecord> openJob;

	@Override
	public void addInitiatedCalculator(final UsageModelPassedElement<Start> event) {
		final User user = event.getContext().getUser();

		addInitialCalculator(event, user);
	}

	@Override
	public void removeFinishedCalculator(final UsageModelPassedElement<Stop> event) {
		final User user = event.getContext().getUser();
		removeFinishedCalculator(user, event.getEntity());
	}

	@Override
	public void addInitiatedCalculator(final SEFFModelPassedElement<StartAction> event) {
		final User user = event.getContext().getRequestProcessingContext().getUser();

		addInitialCalculator(event, user);
	}

	@Override
	public void removeFinishedCalculator(final SEFFModelPassedElement<StopAction> event) {
		final User user = event.getContext().getRequestProcessingContext().getUser();
		removeFinishedCalculator(user, event.getEntity());
	}

	private void addInitialCalculator(final ModelPassedEvent<?> event, final User user) {
		if (!openCalculators.containsKey(user)) {
			openCalculators.put(user, new HashMap<>());
		}
		/*
		 * [S3] Currently, this invariant does not hold. In case of Loops in the
		 * UsageScenario, the UsageModelPassedElement events for Start and Stop might be
		 * delivered in the wrong order.
		 */
		// assert
		// !openCalculators.get(user).containsKey(event.getEntity().eContainer());

		openCalculators.get(user).put(event.getEntity().eContainer(), event);
	}

	private void removeFinishedCalculator(final User user, final Entity entity) {
		if (openCalculators.containsKey(user)) {
			/*
			 * [S3] Currently, the UsageModelPassedElement events for Start and Stop might
			 * be delivered in the wrong order. As result, i cannot ascertain, this
			 * invariant and must instead accept "startless" stops.
			 */
			// assert openCalculators.get(user).containsKey(entity.eContainer()) : "missing
			// start for received stop.";
			if (openCalculators.get(user).containsKey(entity.eContainer())) {
				openCalculators.get(user).remove(entity.eContainer());
			}
		}
	}

	@Override
	public void removeJobRecord(final JobFinished event) {
		openJob.remove(this.getUser(event.getEntity()));
	}

	/**
	 * Helper for accessing the {@link User} instance inside the given {@link Job} instance.
	 *
	 * @param njob job whose user to access
	 * @return the user of the given job
	 */
	private User getUser(final Job njob) {
		if (njob instanceof final ActiveJob job) {
			return job.getRequest().getUser();
		}
		if (njob instanceof final LinkingJob job) {
			return job.getRequest().getUser();
		}
		throw new IllegalArgumentException(
				String.format("Cannot handle jobs of type %s.", njob.getClass().getSimpleName()));
	}

	@Override
	public void createJobRecord(final JobInitiated event) {
		final User user = this.getUser(event.getEntity());

		if (openJob.containsKey(user)) {
			throw new IllegalArgumentException(String.format("Cannot create Record for %s, a Record already exsits.", event.getEntity().toString()));
		}

		openJob.put(user, new JobRecord(event.getEntity()));

	}
	
	/**
	 * Set the normalised demand for the record associated with job in the given event.
	 */
	@Override
	public void updateJobRecord(final JobInitiated event) {
		final User user = this.getUser(event.getEntity());

		if (!openJob.containsKey(user)) {
			throw new IllegalArgumentException(String.format("Cannot update %s, missing Record.", event.getEntity().toString()));
		}

		openJob.get(user).setNormalizedDemand(event.getEntity().getDemand());
	}

	@Override
	public Set<ModelPassedEvent<?>> getRecordedCalculators() {
		final Set<ModelPassedEvent<?>> rval = new HashSet<>();
		openCalculators.values().stream().forEach(adq -> rval.addAll(adq.values()));
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
	 * @return true iff job is processed by an FCFS processing resource.
	 */
	private boolean isFCFS(final Job job) {

		if (job instanceof LinkingJob) {
			return true;
		}
		if (job instanceof final ActiveJob activeJob) {
			return jobIsOfType(activeJob, FCFS_ID);
		}
		LOGGER.debug(String.format("Job of unknown type %s", job.getClass().getSimpleName()));
		return false;
	}

	/**
	 *
	 * @param job
	 * @return true iff job is processed by an processor sharing processing resource
	 */
	private boolean isProcSharing(final Job job) {
		if (job instanceof LinkingJob) {
			return false;
		}
		if (job instanceof final ActiveJob activeJob) {
			return jobIsOfType(activeJob, PROCSHARING_ID);
		}
		LOGGER.debug(String.format("Job of unknown type %s", job.getClass().getSimpleName()));
		return false;
	}

	/**
	 *
	 * @param job
	 * @param type_ID
	 * @return
	 */
	private boolean jobIsOfType(final ActiveJob job, final String type_ID) {
		final Optional<ProcessingResourceSpecification> optSpec = job.getAllocationContext()
				.getResourceContainer_AllocationContext().getActiveResourceSpecifications_ResourceContainer().stream()
				.filter(spec -> spec instanceof ProcessingResourceSpecification).map(spec -> spec).findFirst();

		if (optSpec.isEmpty()) {
			LOGGER.debug(String.format("Missing ProcessingResourceSpecification, cannot determine Type."));
			return false;
		}
		return optSpec.get().getSchedulingPolicy().getId().equals(type_ID);

	}

}
