package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.GeneralEntryRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.resource.CallOverWireRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.resource.ResourceDemandRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.SEFFInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorContextHolder;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.user.RequestProcessingContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.ThinkTime;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.User;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.UserRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.UserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.scenariobehavior.UsageScenarioBehaviorContext;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotTaken;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import spielwiese.version2.EventAndType;
import spielwiese.version2.adapters.ClassTypeAdapter;
import spielwiese.version2.adapters.EObjectTypeAdapter;
import spielwiese.version2.adapters.TypeTokenTypeAdapter;
import spielwiese.version2.factories.ElistTypeAdapterFactory;
import spielwiese.version2.factories.NonParameterizedCustomizedTypeAdapterFactory2;
import spielwiese.version2.factories.OptionalTypeAdapterFactory;
import spielwiese.version2.factories.SEFFBehaviourContextHolderTypeAdapterFactory;

/**
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = DESEvent.class, then = {})
public class SnapshotSerailisationBehavior implements SimulationBehaviorExtension {
	
	private static final Logger LOGGER = Logger.getLogger(SnapshotSerailisationBehavior.class);
	private final boolean activated;
	
	private ResourceSet set;
	private final Map<String, Object> objs = new HashMap<>();
	private final Map<String, TypeAdapter<?>> thingTypes = new HashMap<>();
	
	private final Map<String, Class<? extends DESEvent>> eventTypes = new HashMap<>();
	
	private final Gson gson;
	private final Gson gsonwithAdapter;

	@Inject
	public SnapshotSerailisationBehavior(final @Nullable MDSDBlackboard blackboard) {
		this.activated = true;
		
		this.set = blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getResourceSet();
		
		initialiseEventTypeMap();
		
		
		// Create Gsons
		final GsonBuilder adaptereBuilder = new GsonBuilder();

		// register direct adapters.
		adaptereBuilder.registerTypeHierarchyAdapter(EObject.class, new EObjectTypeAdapter(set));
		adaptereBuilder.registerTypeHierarchyAdapter(Class.class, new ClassTypeAdapter());
		adaptereBuilder.registerTypeHierarchyAdapter(com.google.common.reflect.TypeToken.class, new TypeTokenTypeAdapter());
		
		//register special factory
		adaptereBuilder.registerTypeAdapterFactory(new SEFFBehaviourContextHolderTypeAdapterFactory(thingTypes));

		// register factories
		adaptereBuilder.registerTypeAdapterFactory(new NonParameterizedCustomizedTypeAdapterFactory2(applicableClasses(), objs, thingTypes));
		
		adaptereBuilder.registerTypeAdapterFactory(new OptionalTypeAdapterFactory());
		adaptereBuilder.registerTypeAdapterFactory(new ElistTypeAdapterFactory());
		
		
		
		gsonwithAdapter = adaptereBuilder.create();
		
		final GsonBuilder builder = new GsonBuilder();
		
		builder.registerTypeHierarchyAdapter(EventAndType.class, new JsonDeserializer<DESEvent>() {
			@Override
			public DESEvent deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
					throws JsonParseException {
				
				System.out.println("Event");
				
				if(json.isJsonObject()) {
					final var type = json.getAsJsonObject().get("type");
					final var event = json.getAsJsonObject().get("event");
					if(type != null) {
						final var eventString = type.getAsString();
						if(eventTypes.containsKey(eventString)) {
							return gsonwithAdapter.fromJson(event, eventTypes.get(eventString));
						} else {
							throw new RuntimeException("Invalid message type: " + type);
						}
					}
				}
				throw new RuntimeException("Failed to parse message: " + json);
			}
			
		});
		
		builder.registerTypeHierarchyAdapter(EventAndType.class, new JsonSerializer<EventAndType>() {

			@Override
			public JsonElement serialize(final EventAndType src, final Type typeOfSrc, final JsonSerializationContext context) {
				final JsonObject obj = new JsonObject();
				obj.addProperty("type", src.getType());
				final JsonElement e = gsonwithAdapter.toJsonTree(src.getEvent());
				obj.add("event", e);
				
				if (!eventTypes.containsKey(src.getType())) {
					eventTypes.put(src.getType(), src.getEvent().getClass());
				}
				
				return obj;
			}
			
		});
		
		gson = builder.create();
		
	}

	private void initialiseEventTypeMap() {

		
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	/**
	 * 
	 * @param event
	 */
	@Subscribe
	public void onModelAdjusted(final DESEvent event) {
		
		if (Set.of(SnapshotInitiated.class, SnapshotTaken.class, SnapshotFinished.class).contains(event.getClass())) {
			LOGGER.warn("Skip event " + event.getClass().getSimpleName());
			return;
		}
		
		System.out.println("Attempt to serialize"  + event.getClass().getCanonicalName());
		
		final EventAndType typedEvent = new EventAndType(event, event.getClass().getSimpleName());
		
		final String eventJsonString = gson.toJson(typedEvent);		
		
		System.out.println(eventJsonString);
		
		final Type set2Type = new TypeToken<EventAndType>(){}.getType();

		final DESEvent events2 = gson.fromJson(eventJsonString, set2Type);

		System.out.println("Hello Moon");	
		
		
		
	}

	private Set<Class<?>> applicableClasses() {
		final Set<Class<?>> set = new HashSet<>();
		
		set.add(UsageScenarioBehaviorContext.class);
		set.add(UserInterpretationContext.class);
		set.add(RequestProcessingContext.class);
		set.add(UserRequest.class);
		set.add(User.class); // ????
		
		//currently looking at
		set.add(SEFFInterpretationContext.class);
		set.add(SeffBehaviorWrapper.class);
		set.add(SeffBehaviorContextHolder.class); // we have special TypeAdapter for this one. 
		set.add(ResourceDemandRequest.class);
		set.add(GeneralEntryRequest.class);
		set.add(CallOverWireRequest.class);
		set.add(Job.class);
		
		// might work without
		set.add(ThinkTime.class);
		
		
		return set;
	}
	
}
