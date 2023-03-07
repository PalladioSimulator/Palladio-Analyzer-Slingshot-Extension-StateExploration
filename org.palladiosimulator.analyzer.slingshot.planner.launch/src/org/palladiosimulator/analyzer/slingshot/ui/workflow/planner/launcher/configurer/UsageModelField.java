package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer;

import org.palladiosimulator.analyzer.slingshot.common.constants.model.ModelFileTypeConstants;

public class UsageModelField extends SingleTextModelField {

	public UsageModelField() {
		super(new TextFieldModel("Usage Model", "usageModel", ModelFileTypeConstants.USAGEMODEL_FILE_EXTENSION));
	}

	@Override
	public String fieldName() {
		return "usagemodel";
	}

	@Override
	protected String name() {
		return ModelFileTypeConstants.USAGE_FILE;
	}

}
