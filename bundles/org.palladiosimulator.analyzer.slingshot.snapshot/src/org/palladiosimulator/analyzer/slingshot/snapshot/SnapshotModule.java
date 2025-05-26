package org.palladiosimulator.analyzer.slingshot.snapshot;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;

public class SnapshotModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {
		install(SnapshotInitFromBehaviour.class);
		install(SnapshotInitialAdjustmentBehaviour.class);
		install(SnapshotCostMeasurementsBehaviour.class);
		install(SnapshotStateUpdateBehaviour.class);
		install(OffsetForUsageEvolutionBehaviour.class);
		
		install(SnapshotRecordingBehavior.class);
		
		install(SnapshotTriggeringBehavior.class);
		install(SnapshotSLOTriggeringBehavior.class);
		install(SnapshotAbortionBehavior.class);
		install(PreventPreemptiveSimulationFinished.class);
	}
}
