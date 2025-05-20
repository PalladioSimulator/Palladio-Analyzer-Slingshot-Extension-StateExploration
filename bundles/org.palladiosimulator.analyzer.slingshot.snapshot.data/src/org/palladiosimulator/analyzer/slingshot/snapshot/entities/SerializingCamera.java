package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Camera;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.EventRecord;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

/**
 * 
 * This class creates a {@link Snapshot} based on a the record of a
 * {@link EventRecord} and the future events from the {@link SimulationEngine}.
 * 
 * All events in the snapshot are copies of the originals. Also, delays are
 * already adjusted and offsets for resending those events are encoded into
 * {@link DESEvent#time}.
 * 
 * The copies are created by (de)serializing the events to and from Json.
 * 
 * @author Sophie Stieß
 *
 */
public final class SerializingCamera extends Camera {
	
	private final Path location;
	private final static String fileName = "events.json";
	
	private final PCMResourceSetPartition partition;
	
	public SerializingCamera(final LessInvasiveInMemoryRecord record, final SimulationEngine engine,
			final PCMResourceSetPartition partition, final Collection<SPDAdjustorStateValues> policyIdToValues) {
		super(record, engine, policyIdToValues);

		final String folder = partition.getAllocation().eResource().getURI().trimSegments(1).toFileString();
		final Path path = FileSystems.getDefault().getPath(folder, fileName);

		this.location = path;
		this.partition = partition;
	}

	@Override
	public Snapshot takeSnapshot() {
		this.getScheduledReconfigurations().forEach(this::addEvent);
		final Collection<SPDAdjustorStateValues> values = this.snapStateValues();
			
		final Snapshot snapshot = new InMemorySnapshot(snapEvents(), values);
		return snapshot;
	}

	/**   
	 * TODO
	 */
	public Set<DESEvent> deserialize(final String string) {
		return (new Serializer(partition.getAllocation().eResource().getResourceSet()))
				.deserialize(string);
	}

	/**
	 * Collect and clone all state relevant events from the past and the future and
	 * adjust offsetts, if necessary.
	 *
	 * @return Set of events for recreating the state.
	 */
	private Set<DESEvent> snapEvents() {

		final Set<DESEvent> todoEvents = Set.copyOf(this.collectAndOffsetEvents());
		todoEvents.addAll(additionalEvents); 
		
		final String eventsAsString = this.serializeEvents(todoEvents);
		final Set<DESEvent> clonedEvents = this.deserialize(eventsAsString);
		return clonedEvents;
	}
	
	private String serializeEvents(final Set<DESEvent> events) {	
		return (new Serializer(partition.getAllocation().eResource().getResourceSet()))
				.serialize(events);
	}
	
	/**
	 * 
	 * @param string
	 */
	private void write(final String string) {
		LOGGER.debug("save json to " + location.toFile().toString());

		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(location.toFile()))) {
			writer.write(string);
			writer.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
	
	public String read(final File file) {
		LOGGER.debug("read json from " + file.toString());

		try (final FileReader reader = new FileReader(file)) {
			return Files.readString(file.toPath());
		} catch (final IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
		
}
