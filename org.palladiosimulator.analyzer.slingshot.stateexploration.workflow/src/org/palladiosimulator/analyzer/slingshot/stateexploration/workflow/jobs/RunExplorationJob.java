package org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.jobs;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationDriver;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.DefaultGraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.ExplorationWorkflowConfiguration;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * This class is responsible for starting the planner.
 *
 * @author stiesssh
 */
public class RunExplorationJob implements IBlackboardInteractingJob<MDSDBlackboard> {

	private static final Logger LOGGER = Logger.getLogger(RunExplorationJob.class.getName());

	private MDSDBlackboard blackboard;

	private final SimulationDriver simulationDriver;
	private final PCMResourceSetPartitionProvider pcmResourceSetPartition;
	private final SimuComConfig simuComConfig;

	private final ExplorationWorkflowConfiguration configuration;

	public RunExplorationJob(final ExplorationWorkflowConfiguration config) {
		this.configuration = config;

		this.simulationDriver = Slingshot.getInstance().getSimulationDriver();
		this.pcmResourceSetPartition = Slingshot.getInstance().getInstance(PCMResourceSetPartitionProvider.class);
		this.simuComConfig = config.getSimuComConfig();
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

		final GraphExplorer explorer = new DefaultGraphExplorer(partition, simulationDriver,
				this.configuration.getlaunchConfigParams(), monitor, this.blackboard);
		// Start exploration. On every explored state we send a message, which alloes the external planner component to react
		explorer.start();

		// TODO : decent injection, such that i can hide the implementation class of the explorer.

		//		simulationDriver.init(simuComConfig, monitor);
		monitor.worked(1);

		monitor.subTask("Start simulation");
		//		simulationDriver.start();
		monitor.worked(1);

		monitor.subTask("Restore");
		monitor.worked(1);

		monitor.done();

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
