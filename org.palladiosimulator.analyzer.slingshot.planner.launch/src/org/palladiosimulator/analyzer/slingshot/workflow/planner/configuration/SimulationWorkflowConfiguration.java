package org.palladiosimulator.analyzer.slingshot.workflow.planner.configuration;

import java.util.List;
import java.util.Map;

import org.palladiosimulator.analyzer.workflow.configurations.AbstractPCMWorkflowRunConfiguration;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

public class SimulationWorkflowConfiguration extends AbstractPCMWorkflowRunConfiguration {

	private final ArchitecturalModelsConfiguration inputModels;
	private final SimuComConfig simuConConfig;
	private final String monitorRepositoryFile;
	private final String spdFile;
	private final SlingshotSpecificWorkflowConfiguration slingshotConfig;

	private final Map<String, Object> launchConfigurationParams;

	public SimulationWorkflowConfiguration(final ArchitecturalModelsConfiguration architecturalModels,
			final SimuComConfig config, final SlingshotSpecificWorkflowConfiguration slingshotConfig, final Map<String, Object> launchConfigurationParams) {
		this.inputModels = architecturalModels;
		this.simuConConfig = config; // TODO

		this.launchConfigurationParams = launchConfigurationParams;

		/*
		 * workaround: allocation files are required by the parent class
		 * AbstractPCMWorkflowRunConfiguration.validateAndFreeze when loading PCMModels;
		 * this existence check for PCM models should be done during configuration
		 * validation !!! needs refactoring for simulation it is current not required;
		 * therefore pass empty list in order to successfully execute workflow
		 */
		this.setUsageModelFile(this.inputModels.getUsageModelFile());
		this.setAllocationFiles(List.of(this.inputModels.getAllocationFile()));
		this.monitorRepositoryFile = this.inputModels.getMonitorRepositoryFile();
		this.spdFile = this.inputModels.getSpdFile();
		this.slingshotConfig = slingshotConfig;
	}

	@Override
	public String getErrorMessage() {
		// configuration validation is already done in the LaunchConfiguration class
		// currently no error messages available; return null otherwise workflow
		// validation fails
		return null;
	}

	@Override
	public void setDefaults() {
		// do nothing
	}

	@Override
	public List<String> getPCMModelFiles() {
		// Only returns usage and allocation model files.
		final List<String> modelFiles = super.getPCMModelFiles();
		modelFiles.add(this.monitorRepositoryFile);
		modelFiles.add(this.spdFile);
		return modelFiles;
	}

	public SimuComConfig getConfiguration() {
		return this.simuConConfig;
	}

	public Map<String, Object> getlaunchConfigParams() {
		return this.launchConfigurationParams;
	}

	public SlingshotSpecificWorkflowConfiguration getSlingshotConfiguration() {
		return this.slingshotConfig;
	}

	public ArchitecturalModelsConfiguration getArchitecturalModelsConfiguration() {
		return this.inputModels;
	}

	public String getLogFileName() {
		return this.slingshotConfig.getLogFileName();
	}
}
