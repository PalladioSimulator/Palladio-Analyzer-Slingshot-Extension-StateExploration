package spielwiese.version2.factories;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.GeneralEntryRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.resource.CallOverWireRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.resource.ResourceDemandRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.SEFFInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.BranchBehaviorContextHolder;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.RootBehaviorContextHolder;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorContextHolder;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.user.RequestProcessingContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.ThinkTime;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.User;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.UserRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.ClosedWorkloadUserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.OpenWorkloadUserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.UserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.scenariobehavior.RootScenarioContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.scenariobehavior.UsageScenarioBehaviorContext;
import org.palladiosimulator.analyzer.slingshot.monitor.data.entities.SlingshotMeasuringValue;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementUpdated.MeasurementUpdateInformation;
import org.palladiosimulator.measurementframework.BasicMeasurement;
import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.measurementframework.TupleMeasurement;
import org.palladiosimulator.metricspec.metricentity.MetricEntity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;


/**
 * 
 * @author https://stackoverflow.com/questions/11271375/gson-custom-seralizer-for-one-variable-of-many-in-an-object-using-typeadapter
 * 
 */
public class NonParameterizedCustomizedTypeAdapterFactory2 implements TypeAdapterFactory {

	public static final String FIELD_NAME_CLASS = "class";
	public static final String FIELD_NAME_ID_FOR_REFERENCE = "refId";

	
	private final Set<Class<?>> customizedClasses;
	
	private final Map<String, Object> done;
	private final Map<String, TypeAdapter<?>> thingTypes;
	
	final Set<String> alreadyJsoned = new HashSet<>(); 

	/**
	 * 
	 * @param done
	 * @param thingTypes
	 */
	public NonParameterizedCustomizedTypeAdapterFactory2(final Set<Class<?>> customizables, final Map<String, Object> done, final Map<String, TypeAdapter<?>> thingTypes) {
		this.done = done;
		this.thingTypes = thingTypes;
		this.customizedClasses = customizables;
		
		
	}

	@Override
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (thingTypes.isEmpty()) {
			initThingTypes(gson);
		}
		for (final Class<?> clazz : customizedClasses) {
			if (clazz.isAssignableFrom(type.getRawType())) {
//				final String className = type.getRawType().getSimpleName();
//				if (!thingTypes.containsKey(className)) {
//					thingTypes.put(className, gson.getDelegateAdapter(this, type)); // skips "this" 
//				}
				return customizeMyClassAdapter(gson, type);
			}
		}
		return null;
	}
	
	private void initThingTypes(final Gson gson) {		
		thingTypes.put("UserInterpretationContext", gson.getDelegateAdapter(this, new TypeToken<UserInterpretationContext>() {}));
		thingTypes.put("MetricEntity", gson.getDelegateAdapter(this, new TypeToken<MetricEntity>() {}));
		thingTypes.put("BasicMeasurement", gson.getDelegateAdapter(this, new TypeToken<BasicMeasurement>() {}));
		thingTypes.put("User", gson.getDelegateAdapter(this, new TypeToken<User>() {}));
		thingTypes.put("MeasuringValue", gson.getDelegateAdapter(this, new TypeToken<MeasuringValue>() {}));
		thingTypes.put("SlingshotMeasuringValue", gson.getDelegateAdapter(this, new TypeToken<SlingshotMeasuringValue>() {}));
		thingTypes.put("OpenWorkloadUserInterpretationContext", gson.getDelegateAdapter(this, new TypeToken<OpenWorkloadUserInterpretationContext>() {}));
		thingTypes.put("ClosedWorkloadUserInterpretationContext", gson.getDelegateAdapter(this, new TypeToken<ClosedWorkloadUserInterpretationContext>() {}));
		thingTypes.put("ResourceDemandRequest", gson.getDelegateAdapter(this, new TypeToken<ResourceDemandRequest>() {}));
		thingTypes.put("CallOverWireRequest", gson.getDelegateAdapter(this, new TypeToken<CallOverWireRequest>() {}));
		thingTypes.put("SeffBehaviorWrapper", gson.getDelegateAdapter(this, new TypeToken<SeffBehaviorWrapper>() {}));
		thingTypes.put("SEFFInterpretationContext", gson.getDelegateAdapter(this, new TypeToken<SEFFInterpretationContext>() {}));
		thingTypes.put("RequestProcessingContext", gson.getDelegateAdapter(this, new TypeToken<RequestProcessingContext>() {}));
		thingTypes.put("RootBehaviorContextHolder", gson.getDelegateAdapter(this, new TypeToken<RootBehaviorContextHolder>() {}));
		thingTypes.put("TupleMeasurement", gson.getDelegateAdapter(this, new TypeToken<TupleMeasurement>() {}));
		thingTypes.put("UsageScenarioBehaviorContext", gson.getDelegateAdapter(this, new TypeToken<UsageScenarioBehaviorContext>() {}));
		thingTypes.put("ActiveJob", gson.getDelegateAdapter(this, new TypeToken<ActiveJob>() {}));
		thingTypes.put("BranchBehaviorContextHolder", gson.getDelegateAdapter(this, new TypeToken<BranchBehaviorContextHolder>() {}));
		thingTypes.put("Job", gson.getDelegateAdapter(this, new TypeToken<Job>() {}));
		thingTypes.put("SeffBehaviorContextHolder", gson.getDelegateAdapter(this, new TypeToken<SeffBehaviorContextHolder>() {}));
		thingTypes.put("UserRequest", gson.getDelegateAdapter(this, new TypeToken<UserRequest>() {}));
		thingTypes.put("RootScenarioContext", gson.getDelegateAdapter(this, new TypeToken<RootScenarioContext>() {}));
		thingTypes.put("GeneralEntryRequest", gson.getDelegateAdapter(this, new TypeToken<GeneralEntryRequest>() {})); 

		thingTypes.put("ThinkTime", gson.getDelegateAdapter(this, new TypeToken<ThinkTime>() {})); 
		

		thingTypes.put("MeasurementUpdateInformation", gson.getDelegateAdapter(this, new TypeToken<MeasurementUpdateInformation>() {})); 
	}

	private <R> TypeAdapter<R> customizeMyClassAdapter(final Gson gson, final TypeToken<R> type) {
		final TypeAdapter<R> delegate = gson.getDelegateAdapter(this, type);
		final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
				
		return new TypeAdapter<R>() {
			@Override
			public void write(final JsonWriter out, final R value) throws IOException {
				if (value == null) {
					elementAdapter.write(out, null);
					return;
				}
				
				if (value instanceof String) {
					System.out.println("TODO what about strings?");
					elementAdapter.write(out, new JsonPrimitive((String) value));
				}

				final String refId = String.valueOf(value.hashCode());
				
				if (alreadyJsoned.contains(refId)) {
					elementAdapter.write(out, new JsonPrimitive(refId));
				} else {
					alreadyJsoned.add(refId);
					final JsonObject obj = new JsonObject();

					obj.addProperty(FIELD_NAME_CLASS, value.getClass().getSimpleName());
					obj.addProperty(FIELD_NAME_ID_FOR_REFERENCE, refId);
					
					obj.add("obj", delegate.toJsonTree(value));
					

					elementAdapter.write(out, obj);
				}
			}

			@Override
			public R read(final JsonReader in) throws IOException {
				final JsonElement tree = elementAdapter.read(in);
				if (!tree.isJsonObject() && done.containsKey(tree.getAsString()) ) {
					return (R) done.get(tree.getAsString());
				}
				if (!tree.isJsonObject() && !done.containsKey(tree.getAsString()) ) {
					return null;
				}
				final JsonObject jsonObj = tree.getAsJsonObject();
				
				if (!jsonObj.has(FIELD_NAME_ID_FOR_REFERENCE)) {
					System.out.println("no reference in" + jsonObj.toString());
				}
				
				final String id = jsonObj.get(FIELD_NAME_ID_FOR_REFERENCE).getAsString();
				final String tt = jsonObj.get(FIELD_NAME_CLASS).getAsString();
				
				if (!thingTypes.containsKey(tt)) {
					throw new JsonParseException("Missing Type mapping for " + tt);
				}
				
				final R element = (R) thingTypes.get(tt).fromJsonTree(jsonObj.get("obj"));
				done.put(id, element);
				 
				return element;
			}
		};
	}
}