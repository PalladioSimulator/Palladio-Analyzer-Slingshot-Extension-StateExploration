package org.palladiosimulator.analyzer.slingshot.managedsystem;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.PlanCreatedEventMessage;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.PlanStepAppliedEventMessage;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.SimplifiedPlanCreatedEventMessage;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.StateExploredEventMessage;
import org.palladiosimulator.analyzer.slingshot.networking.data.Message;
import org.palladiosimulator.analyzer.slingshot.networking.data.NetworkingConstants;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

/**
 *
 *
 * @author Sophie Stieß
 *
 */
public class ManagedSystemModule extends AbstractSlingshotExtension {

    @Provides
    @Named(NetworkingConstants.CLIENT_NAME)
    public String clientName() {
        return "ManagedSystem";
    }

    @Override
    protected void configure() {
        // install(SlowdownBehaviour.class);
        install(DelayUntilPlanCreatedBehaviour.class);

//        install(InjectionSystemBehaviour.class);
//        install(InjectionSimulationBehaviour.class);
//        install(SendMessagesBehaviour.class);
        install(RLInjectionSystemBehaviour.class);
        install(RLInjectionSimulationBehaviour.class);
        install(RLSendMessagesBehaviour.class);


        final var messageBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {
        }, new TypeLiteral<Class<? extends Message<?>>>() {
        });

        messageBinder.addBinding(PlanCreatedEventMessage.MESSAGE_MAPPING_IDENTIFIER)
            .toInstance(PlanCreatedEventMessage.class);
        messageBinder.addBinding(StateExploredEventMessage.MESSAGE_MAPPING_IDENTIFIER)
            .toInstance(StateExploredEventMessage.class);

        messageBinder.addBinding(SimplifiedPlanCreatedEventMessage.MESSAGE_MAPPING_IDENTIFIER)
            .toInstance(SimplifiedPlanCreatedEventMessage.class);

        messageBinder.addBinding(PlanStepAppliedEventMessage.MESSAGE_MAPPING_IDENTIFIER)
            .toInstance(PlanStepAppliedEventMessage.class);

        final var gsonBinder = MapBinder.newMapBinder(binder(), Type.class, Object.class);

        gsonBinder.addBinding(ScalingPolicy.class)
            .toInstance(this.createDeserializerForScalingPolicy());
    }

    /**
     *
     * @return deserializer for {@link ScalingPolicy} elements.
     */
    private JsonDeserializer<ScalingPolicy> createDeserializerForScalingPolicy() {

        return new JsonDeserializer<ScalingPolicy>() {

            private SPD spd = null;

            @Override
            public ScalingPolicy deserialize(final JsonElement json, final Type typeOfT,
                    final JsonDeserializationContext context) throws JsonParseException {
                // request a new with each execution, in case the model changed.

                try {
                    spd = Slingshot.getInstance().getInstance(SPD.class);
                } catch (final Exception e) {
                    throw new JsonParseException(String.format(
                            "Cannot deserialise json \"%s\" because SPD model is not available. The model is only available once managed system has started.",
                            json.toString()), e);
                }


                if (spd == null) {
                    throw new JsonParseException(String
                        .format("Cannot deserialise json \"%s\" because SPD model is null.", json.toString()));
                }


//                if (json instanceof final JsonObject object) {
//                    final String id = object.getAsJsonPrimitive("id")
//                        .getAsString();

                if (json instanceof final JsonPrimitive primitive) {
                    final String id = primitive.getAsString();

                    final List<ScalingPolicy> policies = spd.getScalingPolicies();
                    final Optional<ScalingPolicy> matchingPolicy = policies.stream()
                        .filter(p -> p.getId()
                            .equals(id))
                        .findFirst();

                    if (matchingPolicy.isEmpty()) {
                        throw new JsonParseException(String.format(
                                "Cannot deserialise json \"%s\", no matching policy with id \"%s\" in SPD model \"%s\" [id = %s].",
                                json.toString(), id, spd.getEntityName(), spd.getId()));
                    }
                    return matchingPolicy.get();
                }

                throw new JsonParseException(
                        String.format("Json element for policy is not an JsonPrimitive, but \"%s\".", json.toString()));
            }
        };
    }

}
