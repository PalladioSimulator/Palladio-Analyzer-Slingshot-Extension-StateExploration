package org.palladiosimulator.analyzer.slingshot.workflow.planner.launcher.jobs;

import org.eclipse.debug.core.ILaunch;
import org.palladiosimulator.analyzer.slingshot.workflow.planner.configuration.SimulationWorkflowConfiguration;
import org.palladiosimulator.analyzer.workflow.jobs.LoadModelIntoBlackboardJob;
import org.palladiosimulator.analyzer.workflow.jobs.PreparePCMBlackboardPartitionJob;

import de.uka.ipd.sdq.workflow.jobs.ICompositeJob;
import de.uka.ipd.sdq.workflow.jobs.SequentialBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

public class SimulationRootJob extends SequentialBlackboardInteractingJob<MDSDBlackboard> implements ICompositeJob {

	public SimulationRootJob(final SimulationWorkflowConfiguration config, final ILaunch launch) {
		super(SimulationRootJob.class.getName(), false);

		this.addJob(new PreparePCMBlackboardPartitionJob());
		config.getPCMModelFiles()
		.forEach(modelFile -> LoadModelIntoBlackboardJob.parseUriAndAddModelLoadJob(modelFile, this));
		this.addJob(new InitialPlannerJob(config));


	}
}
