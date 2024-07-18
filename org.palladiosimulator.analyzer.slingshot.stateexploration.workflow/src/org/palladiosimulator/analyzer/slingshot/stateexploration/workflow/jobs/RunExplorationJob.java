package org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.jobs;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.TriggerExplorationEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.WorkflowJobDone;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.WorkflowJobStarted;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.ExplorationConfiguration;
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
public class RunExplorationJob implements IBlackboardInteractingJob<MDSDBlackboard> {

	private static final Logger LOGGER = Logger.getLogger(RunExplorationJob.class.getName());

	private MDSDBlackboard blackboard;
	private final PCMResourceSetPartitionProvider pcmResourceSetPartition;

	private final ExplorationWorkflowConfiguration configuration;

	public RunExplorationJob(final ExplorationWorkflowConfiguration config) {
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

		Slingshot.getInstance().getSystemDriver()
		.postEvent(new WorkflowJobStarted(configuration.getPCMModelFiles(),
				this.configuration.getlaunchConfigParams(), monitor, this.blackboard));

		final int iterations = Integer.valueOf((String) this.configuration.getlaunchConfigParams()
				.get(ExplorationConfiguration.MAX_EXPLORATION_CYCLES));

		// [S3] do the valueOf twice, because for some reason the type is either boolean
		// or String, depending on whether the simulator is started headless, or not.
		if (Boolean.valueOf(String.valueOf(this.configuration.getlaunchConfigParams()
				.get(ExplorationConfiguration.IDLE_EXPLORATION)))) {

			final Runnable run = () -> {
				while (true) {
					Slingshot.getInstance().getSystemDriver().postEvent(new TriggerExplorationEvent(1));
				}
			};

			Slingshot.getInstance().getSystemDriver().postEventAndThen(new TriggerExplorationEvent(iterations), run);
		} else {
			Slingshot.getInstance().getSystemDriver().postEvent(new TriggerExplorationEvent(iterations));
		}

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
