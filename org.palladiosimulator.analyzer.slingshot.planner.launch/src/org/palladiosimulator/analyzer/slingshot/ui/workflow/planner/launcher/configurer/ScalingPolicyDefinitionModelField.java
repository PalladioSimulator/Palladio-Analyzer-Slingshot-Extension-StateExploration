package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer;

import org.palladiosimulator.analyzer.slingshot.common.constants.model.ModelFileTypeConstants;

public final class ScalingPolicyDefinitionModelField extends SingleTextModelField {

	public ScalingPolicyDefinitionModelField() {
		super(new TextFieldModel("SPD Model", "Select SPD Model",
				ModelFileTypeConstants.SCALING_POLICY_DEFINITION_FILE_EXTENSION));
	}

	@Override
	public String fieldName() {
		return "scalingPolicyDefinitionField";
	}

	@Override
	protected String name() {
		return ModelFileTypeConstants.SCALING_POLICY_DEFINITION_FILE;
	}

}
