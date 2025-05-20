package org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.jobs;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.WorkflowJobDone;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.SingleStateSimulationExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.ExplorationWorkflowConfiguration;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * This class is responsible for starting the explorer.
 *
 * @author Sarah Stieß
 */
public class RunSingleStateSimulationJob implements IBlackboardInteractingJob<MDSDBlackboard> {

	private static final Logger LOGGER = Logger.getLogger(RunSingleStateSimulationJob.class.getName());

	private MDSDBlackboard blackboard;
	private final PCMResourceSetPartitionProvider pcmResourceSetPartition;

	private final ExplorationWorkflowConfiguration configuration;

	public RunSingleStateSimulationJob(final ExplorationWorkflowConfiguration config) {
		this.configuration = config;
		this.pcmResourceSetPartition = Slingshot.getInstance().getInstance(PCMResourceSetPartitionProvider.class);
	}

	@Override
	public void execute(final IProgressMonitor monitor) throws JobFailedException, UserCanceledException {

		LOGGER.info("**** SimulationJob.execute ****");
		final PCMResourceSetPartition partition = (PCMResourceSetPartition)
				this.blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);

		this.pcmResourceSetPartition.set(partition);
		LOGGER.debug("Current partition: ");
		partition.getResourceSet().getResources().forEach(resource -> LOGGER.debug("Resource: " + resource.getURI().path()));

		LOGGER.debug("monitor: " + monitor.getClass().getName());
		monitor.beginTask("Start Simulation", 3);

		monitor.subTask("Initialize driver");

		EventMessage.EXPLORATION_ID = UUID.randomUUID();

		// get explorere for single state and run the simulation //	
		final SingleStateSimulationExplorer explorer = new SingleStateSimulationExplorer(this.configuration.getlaunchConfigParams(), monitor, this.blackboard);
		explorer.simulateSingleState();

		monitor.worked(1);

		monitor.subTask("Start simulation");
		monitor.worked(1);

		monitor.subTask("Restore");
		monitor.worked(1);

		monitor.done();

		Slingshot.getInstance().getSystemDriver().postEvent(new WorkflowJobDone());

		LOGGER.info("**** SimulationJob.execute  - Done ****");
	}

	@Override
	public void cleanup(final IProgressMonitor monitor) throws CleanupFailedException {

	}

	@Override
	public String getName() {
		return this.getClass().getCanonicalName();
	}

	@Override
	public void setBlackboard(final MDSDBlackboard blackboard) {
		this.blackboard = blackboard;
	}

}
