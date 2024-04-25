package org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.jobs;

import org.eclipse.debug.core.ILaunch;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.PlannerWorkflowConfiguration;
import org.palladiosimulator.analyzer.workflow.jobs.LoadModelIntoBlackboardJob;
import org.palladiosimulator.analyzer.workflow.jobs.PreparePCMBlackboardPartitionJob;

import de.uka.ipd.sdq.workflow.jobs.ICompositeJob;
import de.uka.ipd.sdq.workflow.jobs.SequentialBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

public class PlannerRootJob extends SequentialBlackboardInteractingJob<MDSDBlackboard> implements ICompositeJob {

	public PlannerRootJob(final PlannerWorkflowConfiguration config, final ILaunch launch) {
		super(PlannerRootJob.class.getName(), false);

		this.addJob(new PreparePCMBlackboardPartitionJob());
		config.getPCMModelFiles()
		.forEach(modelFile -> LoadModelIntoBlackboardJob.parseUriAndAddModelLoadJob(modelFile, this));
		this.addJob(new InitialPlannerJob(config));


	}
}
