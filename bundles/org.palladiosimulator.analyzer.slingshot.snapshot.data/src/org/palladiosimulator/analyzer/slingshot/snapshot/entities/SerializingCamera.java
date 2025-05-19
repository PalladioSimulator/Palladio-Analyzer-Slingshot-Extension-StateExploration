package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.ProcessorSharingJobProgressed;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.RepositoryInterpretationInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usageevolution.events.IntervalPassed;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.core.events.PreSimulationConfigurationStarted;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.cost.events.TakeCostMeasurement;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.CalculatorRegistered;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementUpdated;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.ProbeTaken;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.ProcessingTypeRevealed;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.modelvisited.MeasurementSpecificationVisited;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.modelvisited.MonitorModelVisited;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.modelvisited.ProcessingTypeVisited;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Camera;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.EventRecord;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotTaken;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.util.Shareables;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * This class creates a {@link Snapshot} based on a the record of a
 * {@link EventRecord} and the future events from the {@link SimulationEngine}.
 * 
 * All events in the snapshot are copies of the originals. Also, delays are
 * already adjusted and offsets for resending those events are encoded into
 * {@link DESEvent#time}.
 * 
 * 
 * @author Sophie Stieß
 *
 */
public final class SerializingCamera extends Camera {
	
	private final Path location;
	private final String fileName = "events.json";
	
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
		final List<SPDAdjustorStateValues> values = this.snapStateValues();
			
		final Snapshot snapshot = new InMemorySnapshot(snapEvents(), values);
		return snapshot;
	}

	/**   
	 * TODO
	 */
	public Set<DESEvent> read(final File file) {
		return (new Serializer(partition.getAllocation().eResource().getResourceSet()))
				.deserialize(file);
	}

	/**
	 * Collect and clone all state relevant events from the past and the future and
	 * adjust offsetts, if necessary.
	 *
	 * @return Set of events for recreating the state.
	 */
	private Set<DESEvent> snapEvents() {
		this.serializeEvents();
		final Set<DESEvent> clonedEvents = this.read(location.toFile());
		clonedEvents.addAll(additionalEvents); // they are not cloned. maybe problematic? but we didn't clone them earlier either. 
		return clonedEvents;
	}
	
	private String serializeEvents() {	
		final Set<DESEvent> offsettedEvents = collectRelevantEvents();

		return (new Serializer(partition.getAllocation().eResource().getResourceSet()))
				.serialize(offsettedEvents);
	}

	/**
	 * 
	 * @author Sophie Stieß
	 *
	 */
	public class Serializer {

		private final Gson gson;
		
		public Serializer(final ResourceSet set) {
			gson = Shareables.createGsonForSlingshot(set);
		}
		
		
		/**
		 * 
		 * @param events
		 * @return
		 */
		public String serialize(final Set<DESEvent> events) {

			final String eventJsonString = gson.toJson(this.cleanseEventSet(events));

			LOGGER.debug("save json to " + location.toFile().toString());

			try (final BufferedWriter writer = new BufferedWriter(new FileWriter(location.toFile()))) {
				writer.write(eventJsonString);
				writer.flush();
			} catch (final IOException e) {
				e.printStackTrace();
			}

			return eventJsonString;
		}

		/**
		 * 
		 * @param events
		 * @return
		 */
		public Set<DESEvent> deserialize(final File file) {
			LOGGER.debug("read json from " + file.toString());

			try (final FileReader reader = new FileReader(file)) {

				final String readString = Files.readString(file.toPath());
				
				final Type set2Type = new TypeToken<Set<DESEvent>>() {}.getType();

				final Set<DESEvent> events2 = gson.fromJson(readString, set2Type);

				return events2;
			} catch (final IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		/**
		 * Create a set of only those events that ought to be serialised and remove all others. 
		 * 
		 * @param events all events.
		 * @return set of events that ought to be serialized.
		 */
		private Set<DESEvent> cleanseEventSet(final Set<DESEvent> events) {
			final Set<DESEvent> cleansed = new HashSet<>();
			final Set<Class<?>> skip = Set.of(SnapshotInitiated.class, SnapshotTaken.class, SnapshotFinished.class,
					ProbeTaken.class, SimulationFinished.class, MeasurementMade.class, MeasurementUpdated.class, TakeCostMeasurement.class,
					IntervalPassed.class, ProcessorSharingJobProgressed.class);

			final Set<Class<?>> error = Set.of(PreSimulationConfigurationStarted.class, SimulationStarted.class,
					MonitorModelVisited.class, ProcessingTypeRevealed.class, CalculatorRegistered.class,
					RepositoryInterpretationInitiated.class, MeasurementSpecificationVisited.class,
					ProcessingTypeVisited.class, ModelAdjusted.class, ModelAdjustmentRequested.class);

			for (final DESEvent event : events) {
				if (!skip.contains(event.getClass())) {
					cleansed.add(event);
				}
			}
			return cleansed;
		}
	}
}
