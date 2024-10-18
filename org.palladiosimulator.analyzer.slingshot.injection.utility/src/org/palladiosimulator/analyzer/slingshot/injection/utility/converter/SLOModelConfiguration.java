package org.palladiosimulator.analyzer.slingshot.injection.utility.converter;


import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.ui.events.ArchitectureModelsTabBuilderStarted;
import org.palladiosimulator.analyzer.slingshot.workflow.events.WorkflowLaunchConfigurationBuilderInitialized;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;

/**
 *
 * Add a selection field for a {@link ServiceLevelObjective} model instance to
 * the architecture models tab.
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when = ArchitectureModelsTabBuilderStarted.class)
@OnEvent(when = WorkflowLaunchConfigurationBuilderInitialized.class)
public class SLOModelConfiguration implements SystemBehaviorExtension {

	public static final String FILE_NAME = "slo";
	public static final String[] FILE_EXTENSIONS = new String[] { "*.slo" };

	@Subscribe
	public void onArchitectureModelsTab(final ArchitectureModelsTabBuilderStarted event) {
		event.newModelDefinition()
				.fileName(FILE_NAME)
				.fileExtensions(FILE_EXTENSIONS)
				.optional(true)
				.modelClass(ServiceLevelObjective.class)
				.label("Service Level Objectives")
				.build();
	}

	@Subscribe
	public void onWorkflowConfigurationInitialized(final WorkflowLaunchConfigurationBuilderInitialized event) {
		event.getConfiguration(FILE_NAME,
				"",
				(conf, modelFile) -> conf.addOtherModelFile((String) modelFile));
	}

}
