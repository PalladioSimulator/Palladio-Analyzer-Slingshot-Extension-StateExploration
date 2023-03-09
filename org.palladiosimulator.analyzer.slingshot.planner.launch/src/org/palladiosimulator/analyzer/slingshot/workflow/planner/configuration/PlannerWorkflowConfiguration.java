package org.palladiosimulator.analyzer.slingshot.workflow.planner.configuration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.palladiosimulator.analyzer.slingshot.core.api.SimulationConfiguration;
import org.palladiosimulator.analyzer.slingshot.workflow.events.PCMWorkflowConfiguration;
import org.palladiosimulator.analyzer.workflow.configurations.AbstractPCMWorkflowRunConfiguration;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

public class PlannerWorkflowConfiguration extends AbstractPCMWorkflowRunConfiguration implements PCMWorkflowConfiguration, SimulationConfiguration {

		private final SimuComConfig simuComConfig;
		private final Map<String, Object> launchConfigurationParams;
		private List<String> otherFiles;

		public PlannerWorkflowConfiguration(final SimuComConfig configuration, final Map<String, Object> launchConfigurationParams) {
			this.simuComConfig = configuration;
			this.launchConfigurationParams = launchConfigurationParams;
		}

		@Override
		public String getErrorMessage() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setDefaults() {
			// TODO Auto-generated method stub
		}

		@Override
		public SimuComConfig getSimuComConfig() {
			return this.simuComConfig;
		}

		@Override
		public List<String> getPCMModelFiles() {
			final List<String> files = super.getPCMModelFiles();
			if (this.otherFiles != null) {
				files.addAll(this.otherFiles);
			}
			return files;
		}

		@Override
		public void addOtherModelFile(final String modelFile) {
			if (this.otherFiles == null) {
				this.otherFiles = new LinkedList<>();
			}
			this.otherFiles.add(modelFile);
		}

		public Map<String, Object> getlaunchConfigParams() {
			return this.launchConfigurationParams;
		}

	}

