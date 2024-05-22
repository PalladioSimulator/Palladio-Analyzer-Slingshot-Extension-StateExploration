package org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events;

import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;

/**
 * Event to announce a new {@link GraphExplorer} to the
 * {@code ExplorerControllerSystemBehaviour}.
 *
 * Could (should) probably be done using the injection mechanism, instead of an
 * event. [S3]
 *
 * @author Sarah Stieß
 *
 */
public class AnnounceGraphExplorerEvent extends AbstractExplorationControllerEvent {

	private final GraphExplorer explorer;

	public AnnounceGraphExplorerEvent(final GraphExplorer explorer) {
		super();
		if (explorer == null) {
			throw new IllegalArgumentException(String.format("Explorer must not be null but is."));
		}
		this.explorer = explorer;
	}

	public GraphExplorer getExplorer() {
		return explorer;
	}

}
