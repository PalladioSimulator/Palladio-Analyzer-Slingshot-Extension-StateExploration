package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui;

import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.ui.events.ExplorationConfigTabBuilderStarted;
import org.palladiosimulator.analyzer.slingshot.workflow.events.WorkflowLaunchConfigurationBuilderInitialized;

/**
 *
 * Add fields for stateexploration specific settings to the respective tab in
 * the launch configurations.
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when = ExplorationConfigTabBuilderStarted.class)
@OnEvent(when = WorkflowLaunchConfigurationBuilderInitialized.class)
public class ExplorationConfiguration implements SystemBehaviorExtension {

	public static final String MAX_EXPLORATION_CYCLES = "Max Exploration Cycles";
	public static final String MIN_STATE_DURATION = "Min State Duration";
	public static final String SENSIBILITY = "Sensitivity [0, 1 (most sensitive)] ";

	public static final int DEFAULT_MAX_EXPLORATION_CYCLES = 20;
	public static final int DEFAULT_MIN_STATE_DURATION = 20;
	public static final int DEFAULT_SENSIBILITY = 0;

	@Subscribe
	public void onArchitectureModelsTab(final ExplorationConfigTabBuilderStarted event) {
		event.newFieldDefinition()
		.label(MAX_EXPLORATION_CYCLES)
		.promptTitle(String.valueOf(DEFAULT_MAX_EXPLORATION_CYCLES))
		.build();

		event.newFieldDefinition()
		.label(MIN_STATE_DURATION)
		.promptTitle(String.valueOf(DEFAULT_MIN_STATE_DURATION))
		.build();

		event.newFieldDefinition()
		.label(SENSIBILITY)
		.promptTitle(String.valueOf(DEFAULT_SENSIBILITY))
		.build();
	}

	@Subscribe
	public void onWorkflowConfigurationInitialized(final WorkflowLaunchConfigurationBuilderInitialized event) {
		// Do nothing. Values are already in the attributes map.
	}

}
