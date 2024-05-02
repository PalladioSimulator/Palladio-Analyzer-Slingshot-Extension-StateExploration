package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PostIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Camera;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.LessInvasiveInMemoryCamera;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.LessInvasiveInMemoryRecord;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotTaken;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;

/**
 *
 * TODO
 *
 * @author stiesssh
 *
 */
@OnEvent(when = UsageModelPassedElement.class, then = {})
@OnEvent(when = JobFinished.class, then = {})
@OnEvent(when = SnapshotTaken.class, then = SnapshotFinished.class)
@OnEvent(when = SnapshotInitiated.class, then = SnapshotTaken.class)
public class SnapshotRecordingBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotRecordingBehavior.class);
	private static final String FAKE = "fakeID";


	private final LessInvasiveInMemoryRecord recorder;
	private final Camera camera;

	private final SimulationScheduling scheduling;

	@Inject
	public SnapshotRecordingBehavior(final SimulationEngine engine, final Allocation allocation,
			final MonitorRepository monitorRepository, final SimulationScheduling scheduling,
			final PCMResourceSetPartitionProvider set) {
		// can i somehow include this in the injection part?
		// should work with this Model an the 'bind' instruction.

		this.recorder = new LessInvasiveInMemoryRecord();
		this.camera = new LessInvasiveInMemoryCamera(this.recorder, engine, set.get());
		this.scheduling = scheduling;
	}



	@Subscribe(reified = Start.class)
	public void onUsageScenarioStarted(final UsageModelPassedElement<Start> event) {
		this.recorder.addInitiatedCalculator(event);

	}
	@Subscribe(reified = Stop.class)
	public void onUsageScenarioStoped(final UsageModelPassedElement<Stop> event) {
		this.recorder.removeFinishedCalculator(event);
	}

	@Subscribe
	public void removeJobRecord(final JobFinished event) {
		recorder.removeJobRecord(event);
	}

	/**
	 * Create JobRecord before the {@link JobInitiated} get processed, with initial
	 * demand.
	 *
	 * @param information
	 * @param event
	 * @return
	 */
	@PreIntercept
	public InterceptionResult preInterceptSimulationStarted(final InterceptorInformation information,
			final JobInitiated event) {
		if (!event.getEntity().getId().equals(FAKE)) {
			recorder.createJobRecord(event);
		}
		return InterceptionResult.success();
	}

	/**
	 * Update JobRecord after the {@link JobInitiated} got processed, to set
	 * normalized demand.
	 *
	 * @param information
	 * @param event
	 * @param result
	 * @return
	 */
	@PostIntercept
	public InterceptionResult postInterceptSimulationStarted(final InterceptorInformation information,
			final JobInitiated event, final Result<?> result) {
		if (!event.getEntity().getId().equals(FAKE)) {
			recorder.updateJobRecord(event);
		}
		return InterceptionResult.success();
	}

	/**
	 * The actual snapping.
	 *
	 * @param snapshotTaken
	 * @return
	 */
	@Subscribe
	public Result<SnapshotFinished> onSnapshotTakenEvent(final SnapshotTaken snapshotTaken) {
		final Snapshot snapshot = camera.takeSnapshot();

		if (snapshotTaken.getTriggeringEvent().isPresent()) {
			final ModelAdjustmentRequested triggeringeEvent = snapshotTaken.getTriggeringEvent().get();
			snapshot.setModelAdjustmentRequestedEvent(triggeringeEvent);
		}

		return Result.of(new SnapshotFinished(snapshot));
	}

	/**
	 *
	 * Trigger updates to  all processor sharing resource states.
	 *
	 * @param snapshotInitiated
	 * @return
	 */
	@Subscribe
	public Result<SnapshotTaken> onSnapshotInitiatedEvent(final SnapshotInitiated snapshotInitiated) {
		// Cast to ActiveJob is feasible, because LinkingJobs are always FCFS.
		this.scheduleProcSharingUpdatesHelper(
				recorder.getProcSharingJobRecords().stream().map(record -> (ActiveJob) record.getJob())
				.collect(Collectors.toSet()));

		return Result.of(new SnapshotTaken(0, snapshotInitiated.getTriggeringEvent()));
	}

	/**
	 * Schedule exactly on fake {@link JobInitiated} to each {@link AllocationContext} with a processor sharing resource.
	 *
	 * They are scheduled directly to the {@ SimulationScheduling}, to have them posted before the {@link SnapshotTaken}.
	 *
	 * @param procSharingJobs
	 */
	private void scheduleProcSharingUpdatesHelper(final Set<ActiveJob> procSharingJobs) {
		// these are the resource containers i must update
		final Set<AllocationContext> allocationContexts = procSharingJobs.stream()
				.map(job -> job.getAllocationContext()).collect(Collectors.toSet());

		for (final ActiveJob job : procSharingJobs) {
			final AllocationContext context = job.getAllocationContext();
			if (!allocationContexts.contains(context)) {
				continue;
			}
			allocationContexts.remove(context);

			final Job updateJob = this.createFakeJob(job);
			scheduling.scheduleEvent(new JobInitiated(updateJob));
		}
	}

	/**
	 * TODO
	 *
	 * @param job blueprint to copy from
	 * @return fake job
	 */
	private ActiveJob createFakeJob(final ActiveJob job) {
		return ActiveJob.builder().withAllocationContext(job.getAllocationContext()).withDemand(0).withId(FAKE)
				.withProcessingResourceType(job.getProcessingResourceType()).build();
	}
}
