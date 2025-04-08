package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.networking;

import java.lang.reflect.Type;

import javax.inject.Named;

import org.palladiosimulator.analyzer.slingshot.converter.events.StateExploredEventMessage;
import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.networking.data.Message;
import org.palladiosimulator.analyzer.slingshot.networking.data.NetworkingConstants;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.FocusOnStatesEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.ResetExplorerEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.events.TriggerExplorationEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ArchitectureMessageDispatcher;
import org.palladiosimulator.analyzer.slingshot.stateexploration.messages.RequestArchitectureMessage;
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
 * Create bindings for deserializing incoming Messages and install networking
 * related behaviour extensions.
 *
 * Also provides the {@code clientName} for dispatching messages with
 * exploration.
 *
 * @author Raphael Straub, Sarah Stieß
 *
 */
public class ExplorationNetworkingModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {

		configureGson();

		final var messageBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {}, new TypeLiteral<Class<? extends Message<?>>>() {});

		messageBinder.addBinding(StateExploredEventMessage.MESSAGE_MAPPING_IDENTIFIER)
		.toInstance(StateExploredEventMessage.class);

		messageBinder.addBinding(TriggerExplorationEvent.MESSAGE_MAPPING_IDENTIFIER)
		.toInstance(TriggerExplorationEvent.class);

		messageBinder.addBinding(FocusOnStatesEvent.MESSAGE_MAPPING_IDENTIFIER)
		.toInstance(FocusOnStatesEvent.class);

		messageBinder.addBinding(ResetExplorerEvent.MESSAGE_MAPPING_IDENTIFIER)
		.toInstance(ResetExplorerEvent.class);

		messageBinder.addBinding(RequestArchitectureMessage.MESSAGE_MAPPING_IDENTIFIER)
		.toInstance(RequestArchitectureMessage.class);

		install(SimulationUsageDataCollector.class);
		install(ArchitectureMessageDispatcher.class);

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
				jsonScalingPolicy.addProperty("uri", src.eResource().getURI().toString());

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
