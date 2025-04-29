package spielwiese.version2;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.usagemodel.ClosedWorkload;
import org.palladiosimulator.pcm.usagemodel.Delay;
import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.pcm.usagemodel.UsagemodelFactory;
import org.palladiosimulator.pcm.usagemodel.util.UsagemodelResourceImpl;

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

import spielwiese.version2.adapters.ClassTypeAdapter;
import spielwiese.version2.adapters.EObjectTypeAdapter;
import spielwiese.version2.factories.ElistTypeAdapterFactory;
import spielwiese.version2.factories.NonParameterizedCustomizedTypeAdapterFactory2;
import spielwiese.version2.factories.OptionalTypeAdapterFactory;

public class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class);

	private static ResourceSet set = new ResourceSetImpl();
	
	public static void main(final String[] args) {		
		
		final Set<DESEvent> events = createEvents();

		final Set<DESEvent> events2 = Main.deserailize(events);
		
		
		System.out.println("Hello Moon");
	}
	
	
	public static Set<DESEvent> deserailize(final Set<DESEvent> events) {
		// shared data structures
		final Map<String, Object> objs = new HashMap<>();
		final Map<String, TypeAdapter<?>> thingTypes = new HashMap<>();


				
		final GsonBuilder adaptereBuilder = new GsonBuilder();
		adaptereBuilder.registerTypeHierarchyAdapter(EObject.class, new EObjectTypeAdapter(set));
		adaptereBuilder.registerTypeHierarchyAdapter(Class.class, new ClassTypeAdapter());

		//adaptereBuilder.registerTypeAdapterFactory(new CustomizedTypeAdapterFactory<Thing>(Thing.class, objs, thingTypes) {});
		adaptereBuilder.registerTypeAdapterFactory(new NonParameterizedCustomizedTypeAdapterFactory2(objs, thingTypes));
		adaptereBuilder.registerTypeAdapterFactory(new OptionalTypeAdapterFactory(objs, thingTypes));
		adaptereBuilder.registerTypeAdapterFactory(new ElistTypeAdapterFactory());
		
		
		final Gson gsonwithAdapter = adaptereBuilder.create();

		
		final Set<EventAndType> typedEvent = new HashSet<>();
		
		for (final DESEvent e : events) {
			typedEvent.add(new EventAndType(e, e.getClass().getSimpleName()));
		}
	
		
		final Map<String, Class<? extends DESEvent>> eventTypes = new HashMap<>();
		eventTypes.put(SimulationStarted.class.getSimpleName(), SimulationStarted.class);
		eventTypes.put(SimulationFinished.class.getSimpleName(), SimulationFinished.class);
		eventTypes.put(PCMEvent.class.getSimpleName(), PCMEvent.class);
				
		
		final GsonBuilder builder = new GsonBuilder();
		
		builder.registerTypeHierarchyAdapter(EventAndType.class, new JsonDeserializer<DESEvent>() {
			@Override
			public DESEvent deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
					throws JsonParseException {
				
				System.out.println("Event");
				
				if(json.isJsonObject()) {
					final var tyoe = json.getAsJsonObject().get("type");
					final var event = json.getAsJsonObject().get("event");
					if(tyoe != null) {
						final var eventString = tyoe.getAsString();
						if(eventTypes.containsKey(eventString)) {
							return gsonwithAdapter.fromJson(event, eventTypes.get(eventString));
						} else {
							throw new RuntimeException("Invalid message type: " + tyoe);
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
				//obj.add("event", context.serialize(src.getEvent()));
				final JsonElement e = gsonwithAdapter.toJsonTree(src.getEvent());
				obj.add("event", e);
				
				return obj;
			}
			
		});
		
		final Gson gson = builder.create();
		
		
		
		final String eventJsonString = gson.toJson(typedEvent);		
		
		System.out.println(eventJsonString);
		
		final Type set2Type = new TypeToken<HashSet<EventAndType>>(){}.getType();

		return gson.fromJson(eventJsonString, set2Type);
	}
	
	public static UsageModel readUsageModel() {
		createUsageModel(); // for call to eINSTANCES ;)
		final Resource res = new UsagemodelResourceImpl(URI.createURI("file:/var/folders/y4/01qwswz94051py5_hwg72_740000gn/T/77485916-fb6b-4b32-bdc7-aaff0ab64eee/0257b639-ed16-4335-b0e0-abf6e5177491/default.usagemodel"));

		set.getResources().add(res);
		if (res.getContents().isEmpty()) {
			try {
				LOGGER.debug(String.format("Contents of Resource %s was empty and had to be loaded manually.",
						res.getURI().toString()));
				res.unload();
				res.load(((XMLResource) res).getDefaultLoadOptions());
				
				return (UsageModel) res.getContents().get(0);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		} 
		return null;
	}
	
	public static Set<DESEvent> createEvents() {
		
		
		final UsageModel model = readUsageModel();
		
		final Thing thing1 = new Thing("thing1", null);
		final Thing thing2 = new Thing("thing2", thing1);
		final Thing thing3 = new Thing("thing3", thing2);
		
		final PCMThing pcmThing1 = new PCMThing(model, thing1);
		final PCMThing pcmThing2 = new PCMThing(model, thing2);
		
		final Thing thing4 = new Thing("thing4", pcmThing2);
		

		final ClassThing classThing = new ClassThing<Double>(Double.class);
		final ClassThing classThing2 = new ClassThing<Thing>(Thing.class);
		
		final EListThing elistThing = new EListThing(model.getUsageScenario_UsageModel().get(0).getScenarioBehaviour_UsageScenario().getActions_ScenarioBehaviour());
		
		final OptionalThing<Thing> optionalThing = new OptionalThing<>(thing1);
		final OptionalThing<Optional<Thing>> optionalThing2 = new OptionalThing<>(Optional.of(thing1));
		
		final Set<DESEvent> events = new HashSet<>();
		events.add(new SimulationStarted());
//		events.add(new SimulationStarted());
//		events.add(new SimulationFinished());
		events.add(new PCMEvent(model, thing2));
		events.add(new PCMEvent(model, optionalThing));
//		events.add(new PCMEvent(model, pcmThing1));
//		events.add(new PCMEvent(model, pcmThing2));
//		events.add(new PCMEvent(model, pcmThing2));
//		events.add(new PCMEvent(model, thing4));
		
		return events;
		
	}
	
	public static UsageModel createUsageModel() {
		final UsageModel usageModel = UsagemodelFactory.eINSTANCE.createUsageModel();
		final UsageScenario usageScenario = UsagemodelFactory.eINSTANCE.createUsageScenario();

		// workload
		final ClosedWorkload closedWorkload = UsagemodelFactory.eINSTANCE.createClosedWorkload();
		// set bi-directional reference to usage scenario
		closedWorkload.setUsageScenario_Workload(usageScenario);
		closedWorkload.setPopulation(1);
		final PCMRandomVariable thinkTime = CoreFactory.eINSTANCE.createPCMRandomVariable();
		thinkTime.setSpecification("1.0");
		closedWorkload.setThinkTime_ClosedWorkload(thinkTime);

		usageScenario.setWorkload_UsageScenario(closedWorkload);

		// usage behavior
		// entities
		final ScenarioBehaviour behavior = UsagemodelFactory.eINSTANCE.createScenarioBehaviour();
		behavior.setEntityName("scenarioBehavior");
		final Start startEntity = UsagemodelFactory.eINSTANCE.createStart();
		startEntity.setEntityName("start");
		final Delay delayEntity = UsagemodelFactory.eINSTANCE.createDelay();
		final PCMRandomVariable delayTime = CoreFactory.eINSTANCE.createPCMRandomVariable();
		delayTime.setSpecification("1.0");
		delayEntity.setTimeSpecification_Delay(delayTime);
		delayEntity.setEntityName("delay");
		final Stop stopEntity = UsagemodelFactory.eINSTANCE.createStop();
		stopEntity.setEntityName("stop");

		// references
		startEntity.setScenarioBehaviour_AbstractUserAction(behavior);
		startEntity.setSuccessor(delayEntity);
		delayEntity.setSuccessor(stopEntity);
		stopEntity.setPredecessor(delayEntity);

		behavior.setUsageScenario_SenarioBehaviour(usageScenario);
		usageScenario.setScenarioBehaviour_UsageScenario(behavior);
		usageModel.getUsageScenario_UsageModel().add(usageScenario);

		return usageModel;
	}
}