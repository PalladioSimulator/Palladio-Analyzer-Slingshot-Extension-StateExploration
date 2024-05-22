package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui;

import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.ui.events.ExplorationAdditionalConfigTabBuilderStarted;
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
@OnEvent(when = ExplorationAdditionalConfigTabBuilderStarted.class)
@OnEvent(when = WorkflowLaunchConfigurationBuilderInitialized.class)
public class ExplorationConfiguration implements SystemBehaviorExtension {

	public static final String MAX_EXPLORATION_CYCLES = "Max Exploration Cycles";
	public static final String MIN_STATE_DURATION = "Min State Duration";
	public static final String IDLE_EXPLORATION = "Idle Exploration";

	public static final int DEFAULT_MAX_EXPLORATION_CYCLES = 20;
	public static final int DEFAULT_MIN_STATE_DURATION = 20;
	public static final boolean DEFAULT_IDLE_EXPLORATION = false;

	@Subscribe
	public void onExplorationAdditionalConfigTabBuilderStarted(
			final ExplorationAdditionalConfigTabBuilderStarted event) {
		event.newFieldDefinition()
				.label(IDLE_EXPLORATION)
				.defaultValue(DEFAULT_IDLE_EXPLORATION)
				.build();
	}

	@Subscribe
	public void onExplorationConfigTabBuilderStarted(final ExplorationConfigTabBuilderStarted event) {
		event.newFieldDefinition()
		.label(MAX_EXPLORATION_CYCLES)
		.defaultValue(String.valueOf(DEFAULT_MAX_EXPLORATION_CYCLES))
		.build();

		event.newFieldDefinition()
		.label(MIN_STATE_DURATION)
		.defaultValue(String.valueOf(DEFAULT_MIN_STATE_DURATION))
		.build();
	}

	@Subscribe
	public void onWorkflowConfigurationInitialized(final WorkflowLaunchConfigurationBuilderInitialized event) {
		// Do nothing. Values are already in the attributes map.
	}

}
