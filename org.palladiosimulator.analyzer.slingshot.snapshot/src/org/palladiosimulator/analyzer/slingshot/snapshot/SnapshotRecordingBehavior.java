package org.palladiosimulator.analyzer.slingshot.snapshot;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.simulation.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.simulation.events.AbstractEntityChangedEvent;
import org.palladiosimulator.analyzer.slingshot.simulation.extensions.behavioral.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.simulation.extensions.behavioral.annotations.OnEvent;
import org.palladiosimulator.analyzer.slingshot.simulation.extensions.behavioral.results.ResultEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Camera;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.EventRecord;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemoryCamera;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemoryRecord;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotTaken;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;

import com.google.common.eventbus.Subscribe;

/**
 *
 * TODO
 *
 * @author stiesssh
 *
 */
@OnEvent(when = AbstractEntityChangedEvent.class, then = {})
@OnEvent(when = SnapshotTaken.class, then = SnapshotFinished.class)
public class SnapshotRecordingBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotRecordingBehavior.class);

	private final EventRecord recorder;
	private final Camera camera;

	@Inject
	public SnapshotRecordingBehavior(final SimulationEngine engine, final Allocation allocation, final MonitorRepository monitorRepository) {
		// can i somehow include this in the injection part?
		// should work with this Model an the 'bind' instruction.
		this.recorder = new InMemoryRecord();
		this.camera = new InMemoryCamera(recorder, engine, allocation, monitorRepository);
	}

	@Subscribe
	public ResultEvent<?> onAbstractEntityChangedEvent(final AbstractEntityChangedEvent<?> event) {
		recorder.updateRecord(event);
		return ResultEvent.empty();
	}

	/**
	 *
	 * @param snapshotTaken
	 * @return
	 */
	@Subscribe
	public ResultEvent<?> onSnapshotTakenEvent(final SnapshotTaken snapshotTaken) {
		final Snapshot snapshot = camera.takeSnapshot(snapshotTaken.time());
		return ResultEvent.of(new SnapshotFinished(snapshot));
	}
}
