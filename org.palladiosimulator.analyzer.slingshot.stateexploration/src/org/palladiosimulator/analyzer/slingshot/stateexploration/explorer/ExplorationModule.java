package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.lang.reflect.Type;
import java.util.List;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.networking.ws.Message;
import org.palladiosimulator.analyzer.slingshot.planner.data.Measurement;
import org.palladiosimulator.analyzer.slingshot.planner.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.planner.data.Transition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.SLOModelConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.SLOModelProvider;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

public class ExplorationModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {
		install(SLOModelConfiguration.class);
		provideModel(ServiceLevelObjective.class, SLOModelProvider.class);
		
		configureGson();
		
		var messageBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {}, new TypeLiteral<Class<? extends Message<?>>>() {});
		
		messageBinder.addBinding("Test").toInstance(TestMessage.class);
		messageBinder.addBinding("TestSimEvent").toInstance(SimTestMessage.class);
		
		
		messageBinder.addBinding("StateExplored").toInstance(StateExploredMessage.class);
		
		install(ExplorationMessageDispatcher.class);
		install(SimulationUsageDataCollector.class);
		install(SimulationBehaviourReactionTest.class);
	}

	private void configureGson() {
				// Configure Gson
		var gsonBinder = MapBinder.newMapBinder(binder(), Type.class, Object.class);
		
		gsonBinder.addBinding(MeasurementSet.class).toInstance(List.of(
			new JsonDeserializer<MeasurementSet>() {
				@Override
				public MeasurementSet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
						throws JsonParseException {
					
					JsonObject obj = json.getAsJsonObject();
					
					MeasurementSet ms = new MeasurementSet();
					
					ms.setName(obj.get("name").getAsString());
					ms.setMonitorId(obj.get("monitorId").getAsString());
					ms.setMonitorName(obj.get("monitorName").getAsString());
					ms.setSpecificationName(obj.get("specificationName").getAsString());
					ms.setSpecificationId(obj.get("specificationId").getAsString());
					ms.setMetricDescription(obj.get("metricDescription").getAsString());
					ms.setMetricName(obj.get("metricName").getAsString());
					
					for (int i = 0; i < obj.get("elements").getAsJsonArray().size(); i++) {
						JsonObject el = obj.get("elements").getAsJsonArray().get(i).getAsJsonObject();
						
						ms.add(context.deserialize(el, Measurement.class));
					}
					
					return ms;
				}
			}, new JsonSerializer<MeasurementSet>() {  
			    @Override
			    public JsonElement serialize(MeasurementSet src, Type typeOfSrc, JsonSerializationContext context) {
			        JsonObject jsonMeasurementSet = new JsonObject();

			        jsonMeasurementSet.addProperty("name", src.getName());
			        jsonMeasurementSet.addProperty("monitorId", src.getMonitorId());
			        jsonMeasurementSet.addProperty("monitorName", src.getMonitorName());
			        jsonMeasurementSet.addProperty("specificationName", src.getSpecificationName());
			        jsonMeasurementSet.addProperty("specificationId", src.getSpecificationId());
			        jsonMeasurementSet.addProperty("metricName", src.getMetricName());
			        jsonMeasurementSet.addProperty("metricDescription", src.getMetricDescription());
			        jsonMeasurementSet.add("elements", context.serialize(src.toArray()));
			        
			        return jsonMeasurementSet;
			    }
			}));
			
		
		gsonBinder.addBinding(ScalingPolicy.class).toInstance(new JsonSerializer<ScalingPolicy>() {  
		    @Override
		    public JsonElement serialize(ScalingPolicy src, Type typeOfSrc, JsonSerializationContext context) {
		        JsonObject jsonScalingPolicy = new JsonObject();

		        jsonScalingPolicy.addProperty("id", src.getId());
		        jsonScalingPolicy.addProperty("name", src.getEntityName());
		        jsonScalingPolicy.addProperty("uri", src.eResource().getURI().toString());
		        
		        return jsonScalingPolicy;
		    }
		});
		
		
		gsonBinder.addBinding(Transition.class).toInstance(new JsonSerializer<Transition>() {  
		    @Override
		    public JsonElement serialize(Transition src, Type typeOfSrc, JsonSerializationContext context) {
		        JsonObject jsonScalingPolicy = new JsonObject();

		        jsonScalingPolicy.addProperty("targetId", src.target().id());
		        jsonScalingPolicy.addProperty("reason", src.reason().name());
		        
		        return jsonScalingPolicy;
		    }
		});
	}

}
