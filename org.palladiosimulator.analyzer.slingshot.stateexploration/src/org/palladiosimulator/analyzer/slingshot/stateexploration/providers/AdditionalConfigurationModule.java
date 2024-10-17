package org.palladiosimulator.analyzer.slingshot.stateexploration.providers;

import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.DefaultState;

/**
 *
 * Additional provisionings for state exploration.
 *
 * @author Sarah Stieß
 *
 */
public class AdditionalConfigurationModule extends AbstractSlingshotExtension {

	private static final SnapshotConfigurationProvider snapConfigProvider = new SnapshotConfigurationProvider();
	private static final DefaultStateProvider defaultStateProvider = new DefaultStateProvider();
	private static final EventsToInitOnProvider eventsToInitOnProvider = new EventsToInitOnProvider();

	public AdditionalConfigurationModule() {
	}

	@Override
	protected void configure() {
		bind(SnapshotConfiguration.class).toProvider(snapConfigProvider);
		bind(DefaultState.class).toProvider(defaultStateProvider);
		bind(EventsToInitOnWrapper.class).toProvider(eventsToInitOnProvider);

	}

	/**
	 *
	 * Always update these provider together.
	 *
	 * @param config
	 * @param state
	 * @param events
	 */
	public static void updateProviders(final SnapshotConfiguration config, final DefaultState state,
			final Set<DESEvent> events) {
		snapConfigProvider.set(config);
		defaultStateProvider.set(state);
		eventsToInitOnProvider.set(new EventsToInitOnWrapper(events));

	}

	@Override
	public String getName() {
		return "Additional Configuration for Stateexploration";
	}
}
