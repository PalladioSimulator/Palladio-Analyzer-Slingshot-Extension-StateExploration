package spielwiese.version1;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.commons.emfutils.EMFLoadHelper;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class);

	private static ResourceSet set = new ResourceSetImpl();
	
	public static void main(final String[] args) {		
		
		final UsageModel model = readUsageModel();
		
		final Thing thing = new Thing("last thing", null);
		final Thing otherThing = new Thing("previous thing", thing);
		final Thing firstThing = new Thing("first thing", otherThing);
		final Thing otherFirstThing = new Thing("other first thing", otherThing);
		
		final Map<String, Object> objs = new HashMap<>();
		
		final JsonArray jsonArray = new JsonArray();
		final List<JsonElement> failedDeserializationJsonElementList = new ArrayList<>();
		final Set<String> alreadyJsoned = new HashSet<>();		

		final Gson gsonwithAdapter = new GsonBuilder()
			    .registerTypeAdapterFactory(new MyClassTypeAdapterFactory(objs))
			    .create();
		

		
		final Set<DESEvent> events = new HashSet<>();
//		events.add(new SimulationStarted());
//		events.add(new SimulationStarted());
//		events.add(new SimulationFinished());
		events.add(new PCMEvent(model, firstThing));
		events.add(new PCMEvent(model, firstThing));
		events.add(new PCMEvent(model, otherFirstThing));
		
		
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
							return context.deserialize(event, eventTypes.get(eventString));
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
				obj.add("event", context.serialize(src.getEvent()));
				return obj;
			}
			
		});
		
		builder.registerTypeHierarchyAdapter(EObject.class, new JsonDeserializer<EObject>() {
			@Override
			public EObject deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
					throws JsonParseException {
				final String s = json.getAsString();
				final EObject obj = EMFLoadHelper.loadAndResolveEObject(set, s);
				
				return obj;
			}
			
		});
		
		builder.registerTypeHierarchyAdapter(EObject.class, new JsonSerializer<EObject>() {

			@Override
			public JsonElement serialize(final EObject src, final Type typeOfSrc, final JsonSerializationContext context) {
				final URI uri = EcoreUtil.getURI(src);
				return new JsonPrimitive(uri.toString());
			}
			
		});
		
		builder.registerTypeHierarchyAdapter(Thing.class, new JsonDeserializer<Thing>() {
			@Override
			public Thing deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
					throws JsonParseException {
				
				
				if (json.isJsonObject()) {
					final JsonObject jsonObj = json.getAsJsonObject();
					final String id = jsonObj.get("jsonId").getAsString();
					
					final Thing element = gsonwithAdapter.fromJson(json, typeOfT);
					
				 objs.put(id, element);
					
				 return element;


				} else {
						return (Thing) objs.get(json.getAsString());
				}
				
			}
			

			
		});
		
		builder.registerTypeHierarchyAdapter(Thing.class, new JsonSerializer<Thing>() {

			@Override
			public JsonElement serialize(final Thing src, final Type typeOfSrc, final JsonSerializationContext context) {
				final String id = String.valueOf(src.hashCode());
				
				
				final JsonObject element = (JsonObject) gsonwithAdapter.toJsonTree(src);
				objectFuckery(element);
				
				
				return new JsonPrimitive(id);
			}
			
			private void objectFuckery(final JsonObject element) {
				
				if (alreadyJsoned.contains(element.get("jsonId").getAsString())) {
					return;
				}
				
				for (final Map.Entry<String, JsonElement> child : element.asMap().entrySet()) {
					if (child.getValue().isJsonObject()) {
						objectFuckery(child.getValue().getAsJsonObject());
						child.setValue(child.getValue().getAsJsonObject().get("jsonId"));
					}
				}
				
				jsonArray.add(element);
				alreadyJsoned.add(element.get("jsonId").getAsString());
			}
			
		});	
		
		builder.registerTypeHierarchyAdapter(List.class, new TypeAdapter<List<?>>() {
			final TypeAdapter<JsonElement> elementAdapter = gsonwithAdapter.getAdapter(JsonElement.class);
			@Override
			public void write(final JsonWriter out, final List<?> value) throws IOException {
				final JsonElement tree = gsonwithAdapter.toJsonTree(value);
				elementAdapter.write(out, tree);
			}

			@Override
			public List<?> read(final JsonReader in) throws IOException {
				final JsonElement tree = elementAdapter.read(in);
				if (tree.isJsonArray()) {
					final JsonArray array = tree.getAsJsonArray();
					final List<Object> list = new ArrayList<>();
					for (final JsonElement e : array.asList()) {
						try {
							final Object t = gsonwithAdapter.fromJson(e, new TypeToken<Thing>(){}.getType());		
							list.add(t);
						} catch (final JsonSyntaxException ex) {
							failedDeserializationJsonElementList.add(e);
						}
					}					
					return list;
				}
				
				throw new IOException("element is not an json array!");
			}
		});
		
	
		

		final Gson gson = builder.create();
		
			
		final String eventJsonString = gson.toJson(typedEvent);		
		final String objectJsonString = jsonArray.toString();
		
		System.out.println(objectJsonString);
		System.out.println(eventJsonString);
		
		final Type setObjType = new TypeToken<Map<String, Thing>>(){}.getType();
		final Type set2Type = new TypeToken<HashSet<EventAndType>>(){}.getType();

		final Type listAnyType = new TypeToken<List<?>>(){}.getType();

		final List<?> anyObjects = gson.fromJson(objectJsonString, listAnyType);
		final HashSet<DESEvent> events2 = gson.fromJson(eventJsonString, set2Type);

		System.out.println("Hello Moon");
	}
	
	public static UsageModel readUsageModel() {
		createUsageModel(); // for call to eINSTANCES ;)
		final Resource res = new UsagemodelResourceImpl(URI.createURI("file:/var/folders/y4/01qwswz94051py5_hwg72_740000gn/T/57f5fd42-e9d7-41af-8517-2b0793693d4c/89f9a25f-8435-403d-84d0-74140edb335d/default.usagemodel"));

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

class MyClassTypeAdapterFactory extends CustomizedTypeAdapterFactory<Thing> {
	MyClassTypeAdapterFactory(final Map<String, Object> map) {
		super(Thing.class, map);
	}

	@Override
	protected void beforeWrite(final Thing source, final JsonElement toSerialize) {
		if (source != null && toSerialize != null) {
			final JsonObject custom = toSerialize.getAsJsonObject();
			custom.add("jsonId", new JsonPrimitive(String.valueOf(source.hashCode())));
		}
	}

	@Override
	protected void afterRead(final JsonElement deserialized) {
		final JsonObject custom = deserialized.getAsJsonObject();
		custom.remove("jsonId");
	}
}
