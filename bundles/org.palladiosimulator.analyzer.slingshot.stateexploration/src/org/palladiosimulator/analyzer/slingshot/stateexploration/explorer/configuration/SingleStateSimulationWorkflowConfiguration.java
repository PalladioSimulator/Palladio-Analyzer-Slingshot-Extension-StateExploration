package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration;


import java.nio.file.Path;
import java.util.Map;

import org.palladiosimulator.analyzer.slingshot.core.api.SimulationConfiguration;
import org.palladiosimulator.analyzer.slingshot.workflow.events.PCMWorkflowConfiguration;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

public class SingleStateSimulationWorkflowConfiguration extends ExplorationWorkflowConfiguration implements PCMWorkflowConfiguration, SimulationConfiguration {

	private final LocationRecord record;
	

	// i guesss i could also put all those paths into the launchConfigParams. 
	public SingleStateSimulationWorkflowConfiguration(final SimuComConfig configuration, final Map<String, Object> launchConfigurationParams, final LocationRecord record) {
		super(configuration, launchConfigurationParams);
		this.record = record;
	}

	public Path getSnapshotFile() {
		return this.record.snapshotFile;
	}
	
	public Path getResultsFolder() {
		return this.record.resultsFolder;
	}
	
	public Path getOtherConfigsFile() {
		return this.record.otherConfigsFile;
	}

	public record LocationRecord(Path snapshotFile, Path otherConfigsFile, Path resultsFolder) {}
}

