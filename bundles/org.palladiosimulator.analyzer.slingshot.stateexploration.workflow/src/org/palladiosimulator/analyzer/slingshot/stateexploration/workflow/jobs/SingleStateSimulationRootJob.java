package org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.jobs;

import org.eclipse.debug.core.ILaunch;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.ExplorationWorkflowConfiguration;
import org.palladiosimulator.analyzer.workflow.jobs.LoadModelIntoBlackboardJob;
import org.palladiosimulator.analyzer.workflow.jobs.PreparePCMBlackboardPartitionJob;

import de.uka.ipd.sdq.workflow.jobs.ICompositeJob;
import de.uka.ipd.sdq.workflow.jobs.SequentialBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 *
 * Root Job to run an exploration.
 *
 * Prepares the Blackboar, the Partition within the Blackboard, and starts the
 * exploration itself.
 *
 * @author Sarah Stieß
 *
 */
public class SingleStateSimulationRootJob extends SequentialBlackboardInteractingJob<MDSDBlackboard> implements ICompositeJob {

	public SingleStateSimulationRootJob(final ExplorationWorkflowConfiguration config, final ILaunch launch) {
		super(SingleStateSimulationRootJob.class.getName(), false);

		if (launch == null) {
			// Nothing. just stay aware that lunch is null, if it is a headless run.
		}

		this.addJob(new PreparePCMBlackboardPartitionJob());
		config.getPCMModelFiles()
		.forEach(modelFile -> LoadModelIntoBlackboardJob.parseUriAndAddModelLoadJob(modelFile, this));
		// add read snapshot Job?
		this.addJob(new RunSingleStateSimulationJob(config));


	}
}
