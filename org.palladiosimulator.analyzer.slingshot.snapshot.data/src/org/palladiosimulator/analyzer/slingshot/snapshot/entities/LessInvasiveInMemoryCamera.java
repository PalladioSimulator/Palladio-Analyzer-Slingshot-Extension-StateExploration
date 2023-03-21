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
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Camera;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;

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
				.on(DESEvent.class).then(e -> e);
	}

	@Override
	public Snapshot takeSnapshot(final double pointInTime) {
		final Snapshot snapshot = new InMemorySnapshot(snapEvents());
		return snapshot;
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
		final Set<DESEvent> relevantEvents = engine.getScheduledEvents();


		final Set<JobRecord> fcfsRecords = record.getFCFSJobRecords();
		final Set<JobRecord> procsharingRecords = record.getProcSharingJobRecords();

		final Set<AbstractJobEvent> progressedFcfs = relevantEvents.stream().filter(e -> (e instanceof JobProgressed) || (e instanceof JobFinished)).map(e -> (AbstractJobEvent) e).collect(Collectors.toSet());

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
	 *
	 * @param jobrecords
	 * @param fcfsProgressed
	 * @return
	 */
	private Set<JobInitiated> handlePFCFSJobs(final Set<JobRecord> jobrecords, final Set<AbstractJobEvent> fcfsProgressed) {
		final Set<JobInitiated> rval = new HashSet<>();

		final Map<Job, AbstractJobEvent> job2event = new HashMap<>();
		fcfsProgressed.stream().forEach(event -> job2event.put(event.getEntity(), event));

		for (final JobRecord record : jobrecords) {
			if (job2event. keySet().contains(record.getJob()))  {
				final AbstractJobEvent event = job2event.get(record.getJob());
				// time equals remaining demand because of normalization.
				final double remainingDemand = event.time() -  engine.getSimulationInformation().currentSimulationTime();
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
		evt.stream().filter(e -> (e instanceof JobInitiated)).map(e -> (JobInitiated) e).forEach(e -> LOGGER.warn(e.getEntity().getDemand()));
		LOGGER.warn("TIMES");
		evt.stream().filter(e -> (e instanceof UsageModelPassedElement<?>)).map(e -> (UsageModelPassedElement<?>) e).forEach(e -> LOGGER.warn(e.time()));
		LOGGER.warn("CWUI");
		evt.stream().filter(e -> (e instanceof ClosedWorkloadUserInitiated)).map(e -> (ClosedWorkloadUserInitiated) e).forEach(e -> LOGGER.warn(e.delay() + " " + e.time()));
	}
}
