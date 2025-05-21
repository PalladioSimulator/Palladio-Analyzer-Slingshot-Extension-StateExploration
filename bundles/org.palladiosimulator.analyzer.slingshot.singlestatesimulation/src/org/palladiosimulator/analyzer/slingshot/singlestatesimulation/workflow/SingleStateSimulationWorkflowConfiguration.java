package org.palladiosimulator.analyzer.slingshot.singlestatesimulation.workflow;


import java.nio.file.Path;
import java.util.Map;

import org.palladiosimulator.analyzer.slingshot.core.api.SimulationConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.ExplorationWorkflowConfiguration;
import org.palladiosimulator.analyzer.slingshot.workflow.events.PCMWorkflowConfiguration;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

public class SingleStateSimulationWorkflowConfiguration extends ExplorationWorkflowConfiguration implements PCMWorkflowConfiguration, SimulationConfiguration {

	private final Path snapshotLocation;

	public SingleStateSimulationWorkflowConfiguration(final SimuComConfig configuration, final Map<String, Object> launchConfigurationParams, final Path snapshotLocation) {
		super(configuration, launchConfigurationParams);
		this.snapshotLocation = snapshotLocation;
	}

	public Path getSsnapshotLocation() {
		return this.snapshotLocation;
	}

}
