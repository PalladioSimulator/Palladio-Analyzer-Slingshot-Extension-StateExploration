package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.networking;

import java.lang.reflect.Type;

import javax.inject.Named;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.networking.data.Message;
import org.palladiosimulator.analyzer.slingshot.networking.data.NetworkingConstants;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.FocusOnStatesEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.TriggerExplorationEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.messages.GreetingMessage;
import org.palladiosimulator.analyzer.slingshot.stateexploration.messages.SimTestMessage;
import org.palladiosimulator.analyzer.slingshot.stateexploration.messages.StateExploredEventMessage;
import org.palladiosimulator.analyzer.slingshot.stateexploration.messages.TestMessage;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

/**
 *
 *
 *
 *
 *
 * @author Raphael Straub, Sarah Stieß
 *
 */
public class ExplorationNetworkingModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {

		configureGson();

		final var messageBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {}, new TypeLiteral<Class<? extends Message<?>>>() {});

		messageBinder.addBinding(TestMessage.MESSAGE_MAPPING_IDENTIFIER).toInstance(TestMessage.class);
		messageBinder.addBinding(SimTestMessage.MESSAGE_MAPPING_IDENTIFIER).toInstance(SimTestMessage.class);
		messageBinder.addBinding(GreetingMessage.MESSAGE_MAPPING_IDENTIFIER).toInstance(GreetingMessage.class);

		// das hier muss rüber in den planner.
		messageBinder.addBinding(StateExploredEventMessage.MESSAGE_MAPPING_IDENTIFIER)
		.toInstance(StateExploredEventMessage.class);

		messageBinder.addBinding(TriggerExplorationEvent.MESSAGE_MAPPING_IDENTIFIER)
		.toInstance(TriggerExplorationEvent.class);

		messageBinder.addBinding(FocusOnStatesEvent.MESSAGE_MAPPING_IDENTIFIER)
		.toInstance(FocusOnStatesEvent.class);


		// proof of concept -> to be deleted?
		install(ExplorationMessageDispatcher.class);
		install(SimulationUsageDataCollector.class);
		install(SimulationBehaviourReactionTest.class);
	}

	@Provides
	@Named(NetworkingConstants.CLIENT_NAME)
	public String clientName() {
		return "Explorer";
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
				// jsonScalingPolicy.addProperty("uri", src.eResource().getURI().toString());

				return jsonScalingPolicy;
			}
		});

		//		gsonBinder.addBinding(RawModelState.class).toInstance(new JsonSerializer<RawModelState>() {
		//
		//			@Override
		//			public JsonElement serialize(final RawModelState src, final Type typeOfSrc,
		//					final JsonSerializationContext context) {
		//				return new JsonPrimitive(src.getId());
		//			}
		//		});
		//
		//		gsonBinder.addBinding(RawModelState.class).toInstance(new JsonDeserializer<RawModelState>() {
		//
		//			@Override
		//			public RawModelState deserialize(final JsonElement json, final Type typeOfT,
		//					final JsonDeserializationContext context)
		//					throws JsonParseException {
		//				final String foo = json.getAsString();
		//				return null;
		//			}
		//
		//		});
	}

}
