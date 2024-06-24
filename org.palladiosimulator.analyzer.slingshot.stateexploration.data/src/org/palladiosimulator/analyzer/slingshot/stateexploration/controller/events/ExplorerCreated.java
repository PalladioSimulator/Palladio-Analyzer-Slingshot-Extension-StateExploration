package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSystemEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationDriver;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;

import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * Event to announce a new {@link GraphExplorer} to the
 * {@code ExplorerControllerSystemBehaviour}.
 *
 * Could (should) probably be done using the injection mechanism, instead of an
 * event. [S3]
 *
 * @author Sarah Stieß
 *
 */
public class ExplorerCreated extends AbstractSystemEvent implements ExplorationControllerEvent {

	private final List<String> pcmModelFiles;
	private final SimulationDriver driver;
	private final Map<String, Object> launchConfigurationParams;
	private final IProgressMonitor monitor;
	private final MDSDBlackboard blackboard;



	public ExplorerCreated(final List<String> pcmModelFiles, final SimulationDriver driver,
			final Map<String, Object> launchConfigurationParams, final IProgressMonitor monitor,
			final MDSDBlackboard blackboard)
	{
		this.pcmModelFiles = pcmModelFiles;
		this.driver = driver;
		this.launchConfigurationParams = launchConfigurationParams;
		this.monitor = monitor;
		this.blackboard = blackboard;
	}



	public List<String> getPcmModelFiles() {
		return pcmModelFiles;
	}

	public SimulationDriver getDriver() {
		return driver;
	}

	public Map<String, Object> getLaunchConfigurationParams() {
		return launchConfigurationParams;
	}

	public IProgressMonitor getMonitor() {
		return monitor;
	}

	public MDSDBlackboard getBlackboard() {
		return blackboard;
	}
}
