package org.palladiosimulator.analyzer.slingshot.workflow.planner.configuration;

public class ArchitecturalModelsConfiguration {

	private final String allocationFile;
	private final String usageModelFile;
	private final String monitorRepositoryFile;
	private final String spdFile;

	public ArchitecturalModelsConfiguration(final String usageModelFile, final String allocationFile,
			final String monitorRepositoryFile, final String spdFile) {
		this.usageModelFile = usageModelFile;
		this.monitorRepositoryFile = monitorRepositoryFile;
		this.allocationFile = allocationFile;
		this.spdFile = spdFile;
	}

	public String getMonitorRepositoryFile() {
		return this.monitorRepositoryFile;
	}

	public String getAllocationFile() {
		return this.allocationFile;
	}
//	
//	public List<String> getAllocationFiles() {
//		 List<String> copyAllocationFiles = new ArrayList<String>();
//		 copyAllocationFiles.addAll(allocationFiles);
//		 return copyAllocationFiles;
//	}

	public String getUsageModelFile() {
		return this.usageModelFile;
	}

	public String getSpdFile() {
		return this.spdFile;
	}
}