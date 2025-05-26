package org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.PlainSnapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.adapters.EObjectTypeAdapter;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.util.Serializer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.data.InitState;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * TODO 
 * 
 * 
 * @author Sophie Stieß
 *
 */
public final class InitStateDeSerialization implements DeserializeParent<InitState>, SerializeParent<InitState>{

	protected static final Logger LOGGER = Logger.getLogger(InitStateDeSerialization.class);
	
	private final static String SNAPSHOT_FILEDNAME_EVENTS = "events";
	private final static String SNAPSHOT_FILEDNAME_STATEVALUES = "statevalues";
	
	private final PCMResourceSetPartition partition;

	private final Serializer eventSerializer;
	
	private final Gson gson;
	
	public InitStateDeSerialization(final PCMResourceSetPartition partition) {
		super();
		this.partition = partition;
		this.eventSerializer = new Serializer(partition.getAllocation().eResource().getResourceSet());
				
		this.gson = createGson();
	}
	
	@Override
	public InitState deserialize(final Path path) {
		final String read = read(path.toFile());
		return gson.fromJson(read, InitState.class);
	}
	
	@Override
	public void serialize(final InitState snapshot, final Path path) {
		final String json = gson.toJson(snapshot);
		write(json, path.toFile());
	}
	
	
	private Gson createGson() {	
		final GsonBuilder adaptereBuilder = new GsonBuilder();

		// register direct adapters.
		adaptereBuilder.registerTypeHierarchyAdapter(EObject.class, new EObjectTypeAdapter(partition.getAllocation().eResource().getResourceSet()));
		
		adaptereBuilder.registerTypeHierarchyAdapter(Snapshot.class, new SnapshotDeserializer());
		adaptereBuilder.registerTypeHierarchyAdapter(Snapshot.class, new SnapshotSerializer());
		
		return adaptereBuilder.create();
	}
		
	/**
	 * 
	 * @author Sophie Stieß
	 *
	 */
	private class SnapshotDeserializer implements JsonDeserializer<Snapshot> {
		@Override
		public Snapshot deserialize(final JsonElement json, final Type typeOfT,
				final JsonDeserializationContext context) throws JsonParseException {

			if (json.isJsonObject()) {
				final JsonObject jsonobj = json.getAsJsonObject();
				final JsonElement jsonevents = jsonobj.get(SNAPSHOT_FILEDNAME_EVENTS);
				final JsonElement jsonvalues = jsonobj.get(SNAPSHOT_FILEDNAME_STATEVALUES);

				final Type type = new TypeToken<Set<SPDAdjustorStateValues>>() {
				}.getType();

				final Set<DESEvent> events = eventSerializer.deserializeFromJson(jsonevents);
				final Set<SPDAdjustorStateValues> stateValues = context.deserialize(jsonvalues, type);

				return new PlainSnapshot(events, stateValues);
			} else {
				throw new JsonParseException("Expected an JSON object, but found " + json);
			}
		}
	}
	
	/**
	 * 
	 * @author Sophie Stieß
	 *
	 */
	private class SnapshotSerializer implements JsonSerializer<Snapshot> {
		@Override
		public JsonElement serialize(final Snapshot src, final Type typeOfSrc, final JsonSerializationContext context) {

			final Set<DESEvent> events = src.getEvents();
			final Collection<SPDAdjustorStateValues> stateValues = src.getSPDAdjustorStateValues();
			
			final JsonElement eventsJson = eventSerializer.serializeToJson(events);
			final JsonElement stateValuesJson = context.serialize(stateValues);
			
			final JsonObject jsonobj = new JsonObject();
			
			jsonobj.add(SNAPSHOT_FILEDNAME_EVENTS, eventsJson);
			jsonobj.add(SNAPSHOT_FILEDNAME_STATEVALUES, stateValuesJson);
			
			return jsonobj;	
		}
	}
}
