package org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.jobs;

import org.eclipse.debug.core.ILaunch;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.ExplorationWorkflowConfiguration;
import org.palladiosimulator.analyzer.workflow.jobs.LoadModelIntoBlackboardJob;
import org.palladiosimulator.analyzer.workflow.jobs.PreparePCMBlackboardPartitionJob;

import de.uka.ipd.sdq.workflow.jobs.ICompositeJob;
import de.uka.ipd.sdq.workflow.jobs.SequentialBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

public class ExplorationRootJob extends SequentialBlackboardInteractingJob<MDSDBlackboard> implements ICompositeJob {

	public ExplorationRootJob(final ExplorationWorkflowConfiguration config, final ILaunch launch) {
		super(ExplorationRootJob.class.getName(), false);

		if (launch == null) {
			// Nothing. just stay aware that lunch is null, if it is a headless run.
		}

		this.addJob(new PreparePCMBlackboardPartitionJob());
		config.getPCMModelFiles()
				.forEach(modelFile -> LoadModelIntoBlackboardJob.parseUriAndAddModelLoadJob(modelFile, this));
		this.addJob(new RunExplorationJob(config));


	}
}
