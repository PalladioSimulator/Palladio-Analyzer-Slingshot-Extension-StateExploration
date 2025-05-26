package org.palladiosimulator.analyzer.slingshot.stateexploration.providers;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.ExploredStateBuilder;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnProvider;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.SnapshotConfigurationProvider;

/**
 *
 * Additional provisionings for state exploration.
 *
 * @author Sarah Stieß
 *
 */
public class AdditionalConfigurationModule extends AbstractSlingshotExtension{

	public static final SnapshotConfigurationProvider snapConfigProvider = new SnapshotConfigurationProvider();
	public static final SingleStateExploredStateBuilderProvider defaultStateProvider = new SingleStateExploredStateBuilderProvider();
	public static final EventsToInitOnProvider eventsToInitOnProvider = new EventsToInitOnProvider();

	public AdditionalConfigurationModule() {
	}

	@Override
	protected void configure() {
		bind(SnapshotConfiguration.class).toProvider(snapConfigProvider);
		bind(ExploredStateBuilder.class).toProvider(defaultStateProvider);
		bind(EventsToInitOnWrapper.class).toProvider(eventsToInitOnProvider);
	}

	@Override
	public String getName() {
		return "Additional Configuration for Stateexploration";
	}
}
