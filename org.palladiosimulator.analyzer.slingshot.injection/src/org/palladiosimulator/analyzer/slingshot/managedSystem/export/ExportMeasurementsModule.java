package org.palladiosimulator.analyzer.slingshot.managedSystem.export;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.managedSystem.export.messages.MeasurementsExported;
import org.palladiosimulator.analyzer.slingshot.managedSystem.export.messages.MeasurementsRequested;
import org.palladiosimulator.analyzer.slingshot.networking.data.Message;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

/**
 *
 *
 * @author Sarah Stieß
 *
 */
public class ExportMeasurementsModule extends AbstractSlingshotExtension {

    // maybe this should be provided some where else? because i also need it for the export part,
    // but i dont want do include export into injection.
//    @Provides
//    @Named(NetworkingConstants.CLIENT_NAME)
//    public String clientName() {
//        return "ManagedSystem";
//    }

    @Override
    protected void configure() {
        install(ExportSystemBehaviour.class);

        final var messageBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {
        }, new TypeLiteral<Class<? extends Message<?>>>() {
        });

        messageBinder.addBinding(MeasurementsRequested.MESSAGE_MAPPING_IDENTIFIER)
            .toInstance(MeasurementsRequested.class);
        messageBinder.addBinding(MeasurementsExported.MESSAGE_MAPPING_IDENTIFIER)
            .toInstance(MeasurementsExported.class);



        final var gsonBinder = MapBinder.newMapBinder(binder(), Type.class, Object.class);

//        gsonBinder.addBinding(ScalingPolicy.class)
//            .toInstance(this.createDeserializerForScalingPolicy());
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

                if (json instanceof final JsonObject object) {
                    final String id = object.getAsJsonPrimitive("id")
                        .getAsString();

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
                        String.format("Json element for policy is not an JsonObject, but \"%s\".", json.toString()));
            }
        };
    }

}
