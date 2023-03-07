package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer;

import org.palladiosimulator.analyzer.slingshot.common.constants.model.ModelFileTypeConstants;

public final class MonitorRepositoryModelField extends SingleTextModelField {

	public MonitorRepositoryModelField() {
		super(new TextFieldModel("Monitorrepository Model", "Select Monitorrepository Model",
				ModelFileTypeConstants.MONITOR_REPOSITORY_FILE_EXTENSION));
	}

	@Override
	public String fieldName() {
		return "monitorRepositoryModel";
	}

	@Override
	protected String name() {
		return ModelFileTypeConstants.MONITOR_REPOSITORY_FILE;
	}

}
