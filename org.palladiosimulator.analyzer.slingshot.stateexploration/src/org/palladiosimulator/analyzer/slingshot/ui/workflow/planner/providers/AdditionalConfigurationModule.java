package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.providers;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;

public class AdditionalConfigurationModule extends AbstractSlingshotExtension {

	public static final SnapConfigProvider snapConfigProvider = new SnapConfigProvider();
	public static final DefaultStateProvider defaultStateProvider = new DefaultStateProvider();

	public AdditionalConfigurationModule() {
	}

	@Override
	protected void configure() {
		bind(SnapshotConfiguration.class).toProvider(snapConfigProvider);
		bind(DefaultState.class).toProvider(defaultStateProvider);

	}

	@Override
	public String getName() {
		return "Additional Configuration";
	}
}
