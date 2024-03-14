package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.lang.reflect.Type;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.networking.ws.Message;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.SLOModelConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.SLOModelProvider;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
	}

}
