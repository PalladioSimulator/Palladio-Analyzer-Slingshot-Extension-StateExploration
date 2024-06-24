package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSystemEvent;

import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * Event to forward information about the launch to the
 * {@code ExplorerControllerSystemBehaviour}.
 *
 * @author Sarah Stieß
 *
 */
public class LaunchPrepared extends AbstractSystemEvent implements ExplorationControllerEvent {

	private final List<String> pcmModelFiles;
	private final Map<String, Object> launchConfigurationParams;
	private final IProgressMonitor monitor;
	private final MDSDBlackboard blackboard;

	public LaunchPrepared(final List<String> pcmModelFiles,
			final Map<String, Object> launchConfigurationParams, final IProgressMonitor monitor,
			final MDSDBlackboard blackboard) {
		this.pcmModelFiles = pcmModelFiles;
		this.launchConfigurationParams = launchConfigurationParams;
		this.monitor = monitor;
		this.blackboard = blackboard;
	}

	public List<String> getPcmModelFiles() {
		return pcmModelFiles;
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
