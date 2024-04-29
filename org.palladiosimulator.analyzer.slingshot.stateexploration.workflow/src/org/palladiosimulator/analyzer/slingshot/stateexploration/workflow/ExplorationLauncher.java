package org.palladiosimulator.analyzer.slingshot.stateexploration.workflow;

import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.jobs.ExplorationRootJob;
import org.palladiosimulator.analyzer.slingshot.workflow.events.WorkflowLaunchConfigurationBuilderInitialized;
import org.palladiosimulator.analyzer.workflow.configurations.AbstractPCMLaunchConfigurationDelegate;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.jobs.IJob;
import de.uka.ipd.sdq.workflow.logging.console.LoggerAppenderStruct;

public class ExplorationLauncher extends AbstractPCMLaunchConfigurationDelegate<ExplorationWorkflowConfiguration> {

	private final Logger LOGGER = Logger.getLogger(ExplorationLauncher.class);

	private final SystemDriver systemDriver = Slingshot.getInstance().getSystemDriver();

	@Override
	protected IJob createWorkflowJob(final ExplorationWorkflowConfiguration config, final ILaunch launch)
			throws CoreException {
		return new ExplorationRootJob(config, launch);
	}

	@Override
	protected ExplorationWorkflowConfiguration deriveConfiguration(final ILaunchConfiguration configuration,
			final String mode)
					throws CoreException {

		LOGGER.info("PlannerLauncher.deriveConfiguration");

		final SimuComConfig config = new SimuComConfig(configuration.getAttributes(), true);
		final ExplorationWorkflowConfiguration simulationWorkflowConfiguration = new ExplorationWorkflowConfiguration(config, configuration.getAttributes());

		final WorkflowLaunchConfigurationBuilderInitialized builderEvent = new WorkflowLaunchConfigurationBuilderInitialized(configuration, simulationWorkflowConfiguration);
		systemDriver.postEvent(builderEvent);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("The workfloww launch configurations are:");
			builderEvent.forEach().forEach((key, obj) -> {
				LOGGER.debug("Key: " + key + ", Object: " + obj + "<" + obj.getClass().getName() + ">");
			});
		}

		return simulationWorkflowConfiguration;
	}

	@Override
	protected ArrayList<LoggerAppenderStruct> setupLogging(final Level logLevel) throws CoreException {
		// FIXME: during development set debug level hard-coded to DEBUG
		final ArrayList<LoggerAppenderStruct> loggerList = super.setupLogging(Level.DEBUG);
		loggerList.add(this.setupLogger("org.palladiosimulator.analyzer.slingshot", logLevel,
				Level.DEBUG == logLevel ? DETAILED_LOG_PATTERN : SHORT_LOG_PATTERN));
		return loggerList;
	}
}
