package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.utils;

import org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer.RequiredModelConfiguration;
import org.palladiosimulator.analyzer.slingshot.util.extensionpoint.AbstractExtensionPointHandler;

public class ModelConfigurerRegister extends AbstractExtensionPointHandler<RequiredModelConfiguration> {

	public static final String EXTENSION_POINT_ID = "org.palladiosimulator.analyzer.slingshot.configuration.ui.modelregister";
	public static final String EXTENSION_EXECUTION_NAME = "class";
	public static final Class<RequiredModelConfiguration> PROVIDER_CLASS = RequiredModelConfiguration.class;

	@Override
	public String getExtensionPointId() {
		return EXTENSION_POINT_ID;
	}

	@Override
	public String getExecutableExtensionName() {
		return EXTENSION_EXECUTION_NAME;
	}

	@Override
	protected Class<RequiredModelConfiguration> getProvidersClazz() {
		return PROVIDER_CLASS;
	}

}
