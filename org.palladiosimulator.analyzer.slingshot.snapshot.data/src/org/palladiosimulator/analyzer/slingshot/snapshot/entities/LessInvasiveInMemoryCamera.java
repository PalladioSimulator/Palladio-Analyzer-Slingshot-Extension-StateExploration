package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.AbstractJobEvent;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobProgressed;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.ClosedWorkloadUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.InterArrivalUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelper;
import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelperWithVisitor;
import org.palladiosimulator.analyzer.slingshot.behavior.util.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.cost.events.IntervalPassed;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Camera;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;

import de.uka.ipd.sdq.scheduler.resources.active.AbstractActiveResource;

public final class LessInvasiveInMemoryCamera implements Camera {
	private static final Logger LOGGER = Logger.getLogger(LessInvasiveInMemoryCamera.class);

	private final LessInvasiveInMemoryRecord record;
	private final SimulationEngine engine;

	private final LambdaVisitor<DESEvent, DESEvent> adjustOffset;

	public LessInvasiveInMemoryCamera(final LessInvasiveInMemoryRecord record, final SimulationEngine engine) {
		this.record = record;
		this.engine = engine;

		this.adjustOffset = new LambdaVisitor<DESEvent, DESEvent>()
				.on(UsageModelPassedElement.class).then(this::clone)
				.on(ClosedWorkloadUserInitiated.class).then(this::clone)
				.on(InterArrivalUserInitiated.class).then(this::clone)
				.on(IntervalPassed.class).then(this::clone)
				.on(DESEvent.class).then(e -> e);
	}

	@Override
	public Snapshot takeSnapshot() {
		final Snapshot snapshot = new InMemorySnapshot(snapEvents());
		return snapshot;
	}

	private DESEvent clone(final IntervalPassed event) {
		return (new CloneHelper()).clone(event, engine.getSimulationInformation().currentSimulationTime());
	}

	private DESEvent clone(final UsageModelPassedElement<?> event) {
		return (new CloneHelper()).clone(event, engine.getSimulationInformation().currentSimulationTime());
	}

	private DESEvent clone(final ClosedWorkloadUserInitiated event) {
		return (new CloneHelper()).clone(event, engine.getSimulationInformation().currentSimulationTime());
	}

	private DESEvent clone(final InterArrivalUserInitiated event) {
		return (new CloneHelper()).clone(event, engine.getSimulationInformation().currentSimulationTime());
	}

	/**
	 * TODO
	 *
	 * @param pointInTime
	 * @return
	 */
	private Set<DESEvent> snapEvents() {
		// i think this is not smart.
		final Set<DESEvent> relevantEvents = engine.getScheduledEvents();

		final Set<JobRecord> fcfsRecords = record.getFCFSJobRecords();
		final Set<JobRecord> procsharingRecords = record.getProcSharingJobRecords();

		final Set<AbstractJobEvent> progressedFcfs = relevantEvents.stream()
				.filter(e -> (e instanceof JobProgressed) || (e instanceof JobFinished)).map(e -> (AbstractJobEvent) e)
				.collect(Collectors.toSet());

		final Set<JobInitiated> initJobs = new HashSet<>();
		initJobs.addAll(this.handlePFCFSJobs(fcfsRecords, progressedFcfs));
		initJobs.addAll(this.handleProcSharingJobs(procsharingRecords));

		relevantEvents.addAll(initJobs);

		relevantEvents.addAll(record.getRecordedCalculators());

		final Set<DESEvent> offsettedEvents = relevantEvents.stream().map(adjustOffset).collect(Collectors.toSet());
		final Set<DESEvent> clonedEvents = (new CloneHelperWithVisitor()).clone(offsettedEvents);

		this.log(clonedEvents);

		return clonedEvents;
	}

	/**
	 *
	 * @param jobrecords
	 * @return
	 */
	private Set<JobInitiated> handleProcSharingJobs(final Set<JobRecord> jobrecords) {
		final Set<JobInitiated> rval = new HashSet<>();

		for (final JobRecord jobRecord : jobrecords) {
			// do the Proc Sharing Math
			final double ratio = jobRecord.getCurrentDemand() / jobRecord.getNormalizedDemand();
			final double reducedRequested = jobRecord.getRequestedDemand() * ratio;
			jobRecord.getJob().updateDemand(reducedRequested);
			rval.add(new JobInitiated(jobRecord.getJob()));

		}
		return rval;
	}

	/**
	 * Denormalizes the demand of the open jobs and creates {@link JobInitiated}
	 * events to reinsert them to their respective FCFS Resource.
	 *
	 * The demand must be denormalized, because upon receiving a
	 * {@link JobInitiated} event, the {@link AbstractActiveResource} normalizes a
	 * job's demand with the resource's processing rate. Thus without
	 * denormalisation, the demand would be wrong.
	 *
	 * This is required for ActiveJobs, and for LinkingJobs. In case of LinkingJobs,
	 * the throughput is used as processing rate.
	 *
	 * @param jobrecords     jobs waiting at an FCFS resource at the time of the
	 *                       snapshot
	 * @param fcfsProgressed events scheduled for simulation at the time of the
	 *                       snapshot
	 * @return events to reinsert all open jobs to their respective FCFS Resource
	 */
	private Set<JobInitiated> handlePFCFSJobs(final Set<JobRecord> jobrecords,
			final Set<AbstractJobEvent> fcfsProgressed) {
		final Set<JobInitiated> rval = new HashSet<>();

		final Map<Job, AbstractJobEvent> progressedJobs = new HashMap<>();
		fcfsProgressed.stream().forEach(event -> progressedJobs.put(event.getEntity(), event));

		for (final JobRecord record : jobrecords) {
			if (record.getNormalizedDemand() == 0) { // For Linking Jobs.
				if (record.getJob().getDemand() != 0) {
					throw new IllegalStateException(
							String.format("Job %s of Type %s: Normalized demand is 0, but acutal demand is not.",
									record.getJob().toString(), record.getJob().getClass().getSimpleName()));
				}
			} else if (progressedJobs.keySet().contains(record.getJob())) {
				final AbstractJobEvent event = progressedJobs.get(record.getJob());
				// time equals remaining demand because of normalization.
				final double remainingDemand = event.time() - engine.getSimulationInformation().currentSimulationTime();
				final double factor = record.getRequestedDemand() / record.getNormalizedDemand();
				final double denormalizedRemainingDemand = remainingDemand * factor;
				record.getJob().updateDemand(denormalizedRemainingDemand);
			} else {
				record.getJob().updateDemand(record.getRequestedDemand());
			}
			rval.add(new JobInitiated(record.getJob()));
		}
		return rval;
	}

	/**
	 * print information about given set of events.
	 *
	 * @param evt
	 */
	private void log(final Set<DESEvent> evt) {
		LOGGER.warn("DEMANDS");
		evt.stream().filter(e -> (e instanceof JobInitiated)).map(e -> (JobInitiated) e)
		.forEach(e -> LOGGER.warn(e.getEntity().getDemand()));
		LOGGER.warn("TIMES");
		evt.stream().filter(e -> (e instanceof UsageModelPassedElement<?>)).map(e -> (UsageModelPassedElement<?>) e)
		.forEach(e -> LOGGER.warn(e.time()));
		LOGGER.warn("CWUI");
		evt.stream().filter(e -> (e instanceof ClosedWorkloadUserInitiated)).map(e -> (ClosedWorkloadUserInitiated) e)
		.forEach(e -> LOGGER.warn(e.delay() + " " + e.time()));
	}
}
