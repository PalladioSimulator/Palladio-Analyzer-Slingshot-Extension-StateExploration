package org.palladiosimulator.analyzer.slingshot.snapshot.api;

import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.JobRecord;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;

/**
 *
 * A Record holding all Events relevant for recreating a Simulation Run.
 * Currently, those events are:
 *
 * - UsageModelPassedElement for the CalculatorState
 *
 * - JobInitialised for FCFS Resources
 *
 * - JobInitialised for ProcessorSharingResources
 *
 * @author stiesssh
 *
 */
public interface EventRecord {

	/**
	 * Get events, that started a calculation at a calculator and the calculation is
	 * not yet finished, i.e. the respective event to finalize the calculation did
	 * not yet happen.
	 *
	 * @return event to recreate calculator states.
	 */
	public Set<AbstractEntityChangedEvent<?>> getRecordedCalculators();

	/**
	 * Get records for jobs currently processed at a FCFS resource, i.e. the
	 * respective {@JobFinished} event did not yet happen.
	 *
	 * @return records to recreate the state of FCFS resources
	 */
	public Set<JobRecord> getFCFSJobRecords();

	/**
	 * Get records for jobs currently processed at a processor sharing resource,
	 * i.e. the respective {@JobFinished} event did not yet happen.
	 *
	 * @return records to recreate the state of processor sharing resources
	 */
	public Set<JobRecord> getProcSharingJobRecords();


	/**
	 * Store the given event, as it started a calculation at a calculator.
	 *
	 * @param event event that started a calculation.
	 */
	public void addInitiatedCalculator(final UsageModelPassedElement<Start> event);


	/**
	 * Remove event that started the calculation finished by the given event from the record.
	 *
	 * @param event event that finishd a calculation.
	 */
	public void removeFinishedCalculator(final UsageModelPassedElement<Stop> event);

	/**
	 * Create and store record for the job entity in the given event.
	 *
	 * @param event event holding the job entity
	 * @throws IllegalArgumentException
	 */
	public void createJobRecord(final JobInitiated event);

	/**
	 * Update an existing record for the job entity in the given event.
	 *
	 * @param event event holding the job entity
	 * @throws IllegalArgumentException
	 */
	public void updateJobRecord(final JobInitiated event);


	/**
	 * Remove the record of the job entity in the given event, if it exists.
	 *
	 * @param event event holding the job entity
	 */
	public void removeJobRecord(final JobFinished event);

}
