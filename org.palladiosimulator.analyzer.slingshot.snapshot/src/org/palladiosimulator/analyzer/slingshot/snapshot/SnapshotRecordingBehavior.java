package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateExported;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateInitialized;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.SPDAdjustorContext;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.interpreter.entities.SPDAdjustorState;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
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
@OnEvent(when = SPDAdjustorStateExported.class, then = SPDAdjustorStateInitialized.class)
public class SnapshotRecordingBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotRecordingBehavior.class);
	private static final String FAKE = "fakeID";

	/* flag to prevent duplicate snapshots */
	private boolean activated;

	private final LessInvasiveInMemoryRecord recorder;
	private final Camera camera;

	private final Set<SPDAdjustorState> spdAdjustorStates;

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

		this.spdAdjustorStates = new HashSet<>();

		this.activated = true;
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

		snapshot.insertStateInitialisationEvents(this.createStateInitEvents(snapshotTaken.time()));

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

		if (!this.activated) {
			return Result.of();
		}

		this.activated = false;

		// Cast to ActiveJob is feasible, because LinkingJobs are always FCFS.
		this.scheduleProcSharingUpdatesHelper(
				recorder.getProcSharingJobRecords().stream().map(record -> (ActiveJob) record.getJob())
				.collect(Collectors.toSet()));

		return Result.of(new SnapshotTaken(0, snapshotInitiated.getTriggeringEvent()));
	}

	@Subscribe
	public void onAdjustorStateExported(final SPDAdjustorStateExported event) {
		if (event.getEntity() instanceof final SPDAdjustorState state) {
			this.spdAdjustorStates.add(state);
		}
	}

	/**
	 * Schedule exactly one fake {@link JobInitiated} to each
	 * {@link AllocationContext} with a processor sharing resource.
	 *
	 * They are scheduled directly to the {@link SimulationScheduling}, to have them
	 * posted before the {@link SnapshotTaken}.
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
	 * Create a copy of the given job, but replace the id with
	 * {@link SnapshotRecordingBehavior#FAKE}, such that the job can be recognised
	 * and filtered out later on.
	 *
	 * @param job blueprint to copy from
	 * @return fake job
	 */
	private ActiveJob createFakeJob(final ActiveJob job) {
		return ActiveJob.builder().withAllocationContext(job.getAllocationContext()).withDemand(0).withId(FAKE)
				.withProcessingResourceType(job.getProcessingResourceType()).build();
	}

	/**
	 * Creates events for initialising the states of the {@link SPDAdjustorContext}s
	 * for the next simulation run.
	 *
	 * @param referenceTime current simulation time to adjust times in
	 *                      initialisation events.
	 * @return events for initialising the states of the
	 *         {@link SPDAdjustorContext}s.
	 */
	private Collection<DESEvent> createStateInitEvents(final double referenceTime) {
		final Set<DESEvent> set = this.spdAdjustorStates.stream()
				.map(state -> this.mapToInitEvent(state, referenceTime)).collect(Collectors.toSet());
		return set;

	}

	/**
	 *
	 * Create an initialisation event for the given state.
	 *
	 * Times are adjusted, assuming that {@code referenceTime} will be t = 0s for
	 * the next simulation run.
	 *
	 * @param state         state to create an initialisation event for.
	 * @param referenceTime current simulation time to adjust times in
	 *                      initialisation event.
	 * @return a new event for initialisation.
	 */
	private SPDAdjustorStateInitialized mapToInitEvent(final SPDAdjustorState state,final double referenceTime) {
		final double latestAdjustmentAtSimulationTime = state.getLatestAdjustmentAtSimulationTime() - referenceTime;
		final int numberScales = state.numberOfScales();
		final double coolDownEnd = state.getCoolDownEnd() > 0.0 ? state.getCoolDownEnd() - referenceTime : 0.0;
		final int numberOfScalesInCooldown = state.getNumberOfScalesInCooldown();


		return new SPDAdjustorStateInitialized(new SPDAdjustorStateValues(state.getScalingPolicy().getId(),
				latestAdjustmentAtSimulationTime, numberScales, coolDownEnd, numberOfScalesInCooldown));
	}
}
