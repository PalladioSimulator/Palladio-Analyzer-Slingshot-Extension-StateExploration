package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.resource.CallOverWireRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.SEFFInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorWrapper;
import org.palladiosimulator.pcm.seff.impl.StopActionImpl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * 
 * Factory to create {@link TypeAdapter}s for {@link Optional}.
 * 
 * @author Sophie Stieß
 * 
 */
public class OptionalTypeAdapterFactory implements TypeAdapterFactory {
	
	public static final String OPTIONAL_VALUE_FIELD = "value";
	public static final String OPTIONAL_EMPTY = "empty";
	public static final String REFERENCE_FIELD = "ref"; 
	public static final String FIELD_NAME_CLASS = EntityTypeAdapterFactory.FIELD_NAME_CLASS; // must remain equal. 

	private final Map<String, TypeAdapter<?>> optionalValuesDelegators = new HashMap<>();

	public OptionalTypeAdapterFactory() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (type.getRawType() == Optional.class) {
			final TypeAdapter<T> thisAdapter = (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<Class<Optional<? extends Object>>>) type);
			optionalValuesDelegators.put(type.getRawType().getSimpleName(), thisAdapter);
			
			optionalValuesDelegators.put("StopActionImpl", gson.getDelegateAdapter(this, TypeToken.get(StopActionImpl.class)));
			optionalValuesDelegators.put("CallOverWireRequest", gson.getDelegateAdapter(this, TypeToken.get(CallOverWireRequest.class)));
			optionalValuesDelegators.put("SeffBehaviorWrapper", gson.getDelegateAdapter(this, TypeToken.get(SeffBehaviorWrapper.class)));
			optionalValuesDelegators.put("SEFFInterpretationContext", gson.getDelegateAdapter(this, TypeToken.get(SEFFInterpretationContext.class)));
			
			return thisAdapter;
		}
		return null;
	}

	private TypeAdapter<Optional<?>> customizeMyClassAdapter(final Gson gson,
			final TypeToken<Class<Optional<? extends Object>>> type) {
		final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

		final OptionalTypeAdapterFactory forReference = this;

		return new TypeAdapter<Optional<?>>() {
			@Override
			public void write(final JsonWriter out, final Optional<? extends Object> value) throws IOException {
				if (value == null) {
					elementAdapter.write(out, null);
					return;
				}

				final JsonObject obj = new JsonObject();

				obj.addProperty(FIELD_NAME_CLASS, value.getClass().getSimpleName());

				if (value.isPresent()) {

					final String valueClassName = value.get().getClass().getSimpleName();

					// cannot create delegator upfront, becaus we do not know actual value up front.
					if (!optionalValuesDelegators.containsKey(valueClassName)) {
						optionalValuesDelegators.put(valueClassName, gson.getDelegateAdapter(forReference, TypeToken.get(value.get().getClass())));
					}
					final TypeAdapter<Object> delegate = (TypeAdapter<Object>) optionalValuesDelegators.get(valueClassName);
					final JsonElement valueJson =  delegate.toJsonTree(value.get());
					
					if (valueJson.isJsonObject() || valueJson.getAsString().startsWith("file:")) {
						obj.add(OPTIONAL_VALUE_FIELD, valueJson);
					} else {
						final JsonObject refWarp = new JsonObject();
						refWarp.addProperty(FIELD_NAME_CLASS, value.get().getClass().getSimpleName());
						refWarp.add(REFERENCE_FIELD, valueJson);
						obj.add(OPTIONAL_VALUE_FIELD, refWarp);
					}

				} else {
					obj.addProperty(OPTIONAL_VALUE_FIELD, OPTIONAL_EMPTY);
				}

				elementAdapter.write(out, obj);

			}

			@Override
			public Optional<?> read(final JsonReader in) throws IOException {
				final JsonElement tree = elementAdapter.read(in);

				final JsonObject jsonObj = tree.getAsJsonObject();
				final JsonElement value = jsonObj.get(OPTIONAL_VALUE_FIELD);

				final Optional<?> element;

				if (value.isJsonObject()) {
					final String innerTt = value.getAsJsonObject().get(FIELD_NAME_CLASS).getAsString();
					
					if (!optionalValuesDelegators.containsKey(innerTt)) {
						throw new JsonParseException("Missing delegate for optional values of type" + innerTt);
					}
					
					final TypeAdapter<Object> delegate = (TypeAdapter<Object>) optionalValuesDelegators.get(innerTt);
					
					
					if (value.getAsJsonObject().has(REFERENCE_FIELD)) {
						element = Optional.of(delegate.fromJsonTree(value.getAsJsonObject().get(REFERENCE_FIELD)));
					} else {
						element = Optional.of(delegate.fromJsonTree(value));
					}

				} else if (value.isJsonArray()) {
					throw new JsonParseException("Unexpected and unhandled primitive inside Optional: " + value.toString());
				} else {
					if (value.getAsString().equals(OPTIONAL_EMPTY)) {
						return Optional.empty();
					} else if (value.getAsString().startsWith("file:")) {
						element = Optional.of(gson.fromJson(value, EObject.class));
					} else {
						throw new JsonParseException("Unexpected and unhandled primitive inside Optional: " + value.toString());
					}
				}

				return element;
			}
		};
	}
}