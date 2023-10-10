package org.palladiosimulator.analyzer.slingshot.stateexploration.graphicalrepresentation;

import java.lang.reflect.Type;

import org.palladiosimulator.analyzer.slingshot.planner.data.Measurement;
import org.palladiosimulator.analyzer.slingshot.planner.data.MeasurementSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonProvider {
	private static GsonBuilder gsonBuilder = null;
	private static Gson gson = null;
	
	public static synchronized Gson getGson() {
		if (gson == null) {
			JsonDeserializer<MeasurementSet> deserializerMeasurementSet = new JsonDeserializer<MeasurementSet>() {
				@Override
				public MeasurementSet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
						throws JsonParseException {
					
					JsonObject obj = json.getAsJsonObject();
					
					MeasurementSet ms = new MeasurementSet();
					
					ms.setName(obj.get("name").getAsString());
					ms.setMeasuringPointURI(obj.get("measuringPointURI").getAsString());
					
					for (int i = 0; i < obj.get("elements").getAsJsonArray().size(); i++) {
						JsonObject el = obj.get("elements").getAsJsonArray().get(i).getAsJsonObject();
						
						ms.add(context.deserialize(el, Measurement.class));
					}
					
					return ms;
				}
			};
			
			JsonSerializer<MeasurementSet> serializerMeasurementSet = new JsonSerializer<MeasurementSet>() {  
			    @Override
			    public JsonElement serialize(MeasurementSet src, Type typeOfSrc, JsonSerializationContext context) {
			        JsonObject jsonMeasurementSet = new JsonObject();

			        jsonMeasurementSet.addProperty("name", src.getName());
			        jsonMeasurementSet.addProperty("measuringPointURI", src.getMeasuringPointURI());
			        jsonMeasurementSet.add("elements", context.serialize(src.toArray()));
			        
			        return jsonMeasurementSet;
			    }
			};

			gsonBuilder = new GsonBuilder();
			gsonBuilder.serializeNulls();
			gsonBuilder.registerTypeAdapter(MeasurementSet.class, deserializerMeasurementSet);
			gsonBuilder.registerTypeAdapter(MeasurementSet.class, serializerMeasurementSet);
			
			gson = gsonBuilder.create();
		}
		
		return gson;
	}
}
