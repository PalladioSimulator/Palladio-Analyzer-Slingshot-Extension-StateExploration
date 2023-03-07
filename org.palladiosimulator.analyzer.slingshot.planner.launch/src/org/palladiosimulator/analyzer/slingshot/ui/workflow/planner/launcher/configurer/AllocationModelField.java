package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer;

import org.palladiosimulator.analyzer.slingshot.common.constants.model.ModelFileTypeConstants;

public class AllocationModelField extends SingleTextModelField {

	public AllocationModelField() {
		super(new TextFieldModel("Allocation Model", "allocationmodel",
				ModelFileTypeConstants.ALLOCATION_FILE_EXTENSION));
	}

	@Override
	public String fieldName() {
		return "allocationmodel";
	}

	@Override
	protected String name() {
		return ModelFileTypeConstants.ALLOCATION_FILE;
	}

}
