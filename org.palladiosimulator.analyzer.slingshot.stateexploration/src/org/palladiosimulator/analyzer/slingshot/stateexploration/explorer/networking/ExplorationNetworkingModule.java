package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.networking;

import java.lang.reflect.Type;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.networking.data.Message;
import org.palladiosimulator.analyzer.slingshot.planner.data.events.StateExploredEventMessage;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.networking.messages.SimTestMessage;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.networking.messages.TestMessage;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

public class ExplorationNetworkingModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {

		configureGson();

		final var messageBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {}, new TypeLiteral<Class<? extends Message<?>>>() {});

		messageBinder.addBinding("Test").toInstance(TestMessage.class);
		messageBinder.addBinding("TestSimEvent").toInstance(SimTestMessage.class);


		messageBinder.addBinding("StateExplored").toInstance(StateExploredEventMessage.class);

		install(ExplorationMessageDispatcher.class);
		install(SimulationUsageDataCollector.class);
		install(SimulationBehaviourReactionTest.class);
	}

	private void configureGson() {
		// Configure Gson
		final var gsonBinder = MapBinder.newMapBinder(binder(), Type.class, Object.class);

		gsonBinder.addBinding(ScalingPolicy.class).toInstance(new JsonSerializer<ScalingPolicy>() {
			@Override
			public JsonElement serialize(final ScalingPolicy src, final Type typeOfSrc, final JsonSerializationContext context) {
				final JsonObject jsonScalingPolicy = new JsonObject();

				jsonScalingPolicy.addProperty("id", src.getId());
				jsonScalingPolicy.addProperty("name", src.getEntityName());
				jsonScalingPolicy.addProperty("uri", src.eResource().getURI().toString());

				return jsonScalingPolicy;
			}
		});
	}

}
