package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.ProcessorSharingJobProgressed;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.RepositoryInterpretationInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usageevolution.events.IntervalPassed;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
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
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotTaken;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.util.Shareables;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

/** 
 * Serializer for (de)serializing {@link DESEvent} from and to JSON.
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
	 * Serialize given events to JSON
	 * 
	 * @param events
	 * @return JSON of the events as string.
	 */
	public String serialize(final Set<DESEvent> events) {
		final String eventJsonString = gson.toJson(this.cleanseEventSet(events));
		return eventJsonString;
	}
	
	/**
	 * Serialize given events to JSON
	 * 
	 * @param events
	 * @return JSON of the events.
	 */
	public JsonElement serializeToJson(final Set<DESEvent> events) {
		final JsonElement eventJsonString = gson.toJsonTree(this.cleanseEventSet(events));
		return eventJsonString;
	}

	/**
	 * Deserialize events from the given string.
	 * 
	 * @param string JSON as string to be deserialized
	 * @return Set of all events from the string.
	 */
	public Set<DESEvent> deserialize(final String string) {
		final Type set2Type = new TypeToken<Set<DESEvent>>() {
		}.getType();
		return gson.fromJson(string, set2Type);
	}
	
	/**
	 * Deserialize events from the given JSON.
	 *
	 * @param element JSON to be deserialized
	 * @return Set of all events from the JSON.
	 */
	public Set<DESEvent> deserializeFromJson(final JsonElement element) {
		final Type type = new TypeToken<Set<DESEvent>>() {
		}.getType();
		return gson.fromJson(element, type);
	}

	/**
	 * Create a set of only those events that ought to be serialised and remove all
	 * others.
	 * 
	 * @param events all events.
	 * @return set of events that ought to be serialized.
	 */
	private Set<DESEvent> cleanseEventSet(final Set<DESEvent> events) {
		final Set<DESEvent> cleansed = new HashSet<>();
		final Set<Class<?>> skip = Set.of(SnapshotInitiated.class, SnapshotTaken.class, SnapshotFinished.class,
				ProbeTaken.class, SimulationFinished.class, MeasurementMade.class, MeasurementUpdated.class,
				TakeCostMeasurement.class, IntervalPassed.class, ProcessorSharingJobProgressed.class);

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
