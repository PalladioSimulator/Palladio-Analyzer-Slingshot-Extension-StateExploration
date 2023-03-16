package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.common.utils.events.ModelPassedEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
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

/**
 *
 * TODO
 *
 * @author stiesssh
 *
 */
@OnEvent(when = ModelPassedEvent.class, then = {})
@OnEvent(when = JobFinished.class, then = {})
@OnEvent(when = SnapshotTaken.class, then = SnapshotFinished.class)
@OnEvent(when = SnapshotInitiated.class, then = SnapshotTaken.class)
public class SnapshotRecordingBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotRecordingBehavior.class);

	private final LessInvasiveInMemoryRecord recorder;
	private final Camera camera;

	private final SimulationScheduling scheduling;

	@Inject
	public SnapshotRecordingBehavior(final SimulationEngine engine, final Allocation allocation,
			final MonitorRepository monitorRepository, final SimulationScheduling scheduling) {
		// can i somehow include this in the injection part?
		// should work with this Model an the 'bind' instruction.
		// this.recorder = new InMemoryRecord();
		// this.camera = new InMemoryCamera(recorder, engine, allocation,
		// monitorRepository);
		this.recorder = new LessInvasiveInMemoryRecord();
		this.camera = new LessInvasiveInMemoryCamera(this.recorder, engine);
		this.scheduling = scheduling;
	}

	@Subscribe
	public void onModelPassedEvent(final ModelPassedEvent<?> event) {
		recorder.updateRecord(event);
	}

	@Subscribe
	public void onJobFinished(final JobFinished event) {
		recorder.updateRecord(event);
	}

	@PreIntercept
	public InterceptionResult preInterceptSimulationStarted(final InterceptorInformation information,
			final JobInitiated event) {
		recorder.updateRecord(event);
		return InterceptionResult.success();
	}

	@PostIntercept
	public InterceptionResult postInterceptSimulationStarted(final InterceptorInformation information,
			final JobInitiated event, final Result<?> result) {
		// not needed, if i could guarantee, that resource simulation get's the job
		// first.
		recorder.updateRecord(event);
		return InterceptionResult.success();
	}

	/**
	 *
	 * @param snapshotTaken
	 * @return
	 */
	@Subscribe
	public Result<SnapshotFinished> onSnapshotTakenEvent(final SnapshotTaken snapshotTaken) {
		final Snapshot snapshot = camera.takeSnapshot(snapshotTaken.time());
		return Result.of(new SnapshotFinished(snapshot));
	}

	@Subscribe
	public Result<SnapshotTaken> onSnapshotInitiatedEvent(final SnapshotInitiated snapshotInitiated) {
		this.scheduleProcSharingUpdatesHelper(
				recorder.getProcSharingJobRecords().stream().map(record -> record.getJob()).collect(Collectors.toSet()));

		return Result.of(new SnapshotTaken());
	}

	private void scheduleProcSharingUpdatesHelper(final Set<Job> procSharingJobs) {
		// these are the resource containers i must update
		final Set<AllocationContext> allocationContexts = procSharingJobs.stream()
				.map(job -> job.getAllocationContext()).collect(Collectors.toSet());

		for (final Job job : procSharingJobs) {
			final AllocationContext context = job.getAllocationContext();
			if (!allocationContexts.contains(context)) {
				continue;
			}
			allocationContexts.remove(context);

			final Job updateJob = recorder.getUpdateJob(job);
			scheduling.scheduleEvent(new JobInitiated(updateJob));
		}
	}
}
