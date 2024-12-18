package org.palladiosimulator.analyzer.slingshot.snapshot;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;

public class SnapshotModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {
		install(SnapshotRecordingBehavior.class);
		install(SnapshotGraphStateBehaviour.class);
		install(SnapshotTriggeringBehavior.class);
		install(SnapshotSLOTriggeringBehavior.class);
	}
}
