package org.palladiosimulator.analyzer.slingshot.stateexploration.workflow;


import java.util.Map;

import org.palladiosimulator.analyzer.slingshot.core.api.SimulationConfiguration;
import org.palladiosimulator.analyzer.slingshot.workflow.SimulationWorkflowConfiguration;
import org.palladiosimulator.analyzer.slingshot.workflow.events.PCMWorkflowConfiguration;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

public class PlannerWorkflowConfiguration extends SimulationWorkflowConfiguration implements PCMWorkflowConfiguration, SimulationConfiguration {

	private final Map<String, Object> launchConfigurationParams;

	public PlannerWorkflowConfiguration(final SimuComConfig configuration, final Map<String, Object> launchConfigurationParams) {
		super(configuration);
		this.launchConfigurationParams = launchConfigurationParams;
	}

	public Map<String, Object> getlaunchConfigParams() {
		return this.launchConfigurationParams;
	}

}
