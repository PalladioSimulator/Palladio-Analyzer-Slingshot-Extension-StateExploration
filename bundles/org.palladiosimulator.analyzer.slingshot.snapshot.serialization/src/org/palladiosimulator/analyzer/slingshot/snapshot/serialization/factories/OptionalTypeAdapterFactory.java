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
//	public static final String REFERENCE_FIELD = EntityTypeAdapterFactory.FIELD_NAME_ID_FOR_REFERENCE; // must remain equal.  
	public static final String FIELD_NAME_CLASS = EntityTypeAdapterFactory.FIELD_NAME_CLASS; // must remain equal. 

	private final Map<String, TypeAdapter<?>> optionalValuesDelegators = new HashMap<>();

	public OptionalTypeAdapterFactory() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (type.getRawType() == Optional.class) {
			final TypeAdapter<T> thisAdapter = (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<Class<Optional<? extends Object>>>) type);
			optionalValuesDelegators.put(type.getRawType().getCanonicalName(), thisAdapter);
			
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

		return new TypeAdapter<Optional<?>>() {
			@Override
			public void write(final JsonWriter out, final Optional<? extends Object> optional) throws IOException {
				if (optional == null) {
					elementAdapter.write(out, null);
					return;
				}

				final JsonObject jsonObject = new JsonObject();

				jsonObject.addProperty(FIELD_NAME_CLASS, optional.getClass().getCanonicalName());

				if (optional.isPresent()) {

					final String valueClassName = optional.get().getClass().getCanonicalName();

					
					final TypeAdapter<Object> delegate = this.getDelegator(gson, optional, valueClassName);
					final JsonElement valueJson =  delegate.toJsonTree(optional.get());
					
					if (valueJson.isJsonObject() || valueJson.getAsString().startsWith("file:")) {
						jsonObject.add(OPTIONAL_VALUE_FIELD, valueJson);
					} else {
						final JsonObject refWarp = new JsonObject();
						refWarp.addProperty(FIELD_NAME_CLASS, optional.get().getClass().getCanonicalName());
						refWarp.add(REFERENCE_FIELD, valueJson);
						jsonObject.add(OPTIONAL_VALUE_FIELD, refWarp);
					}

				} else {
					jsonObject.addProperty(OPTIONAL_VALUE_FIELD, OPTIONAL_EMPTY);
				}

				elementAdapter.write(out, jsonObject);

			}

			/**
			 * 
			 * @param gson
			 * @param optional
			 * @param valueClassName
			 * @return
			 */
			private TypeAdapter<Object> getDelegator(final Gson gson, final Optional<? extends Object> optional,
					final String valueClassName) {
				if (!optionalValuesDelegators.containsKey(valueClassName)) {
					optionalValuesDelegators.put(valueClassName, gson.getDelegateAdapter(OptionalTypeAdapterFactory.this, TypeToken.get(optional.get().getClass())));
				}
				final TypeAdapter<Object> delegate = (TypeAdapter<Object>) optionalValuesDelegators.get(valueClassName);
				return delegate;
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
						throw new JsonParseException("Missing delegate for value of type" + innerTt + "inside an Optional");
					}
					
					final TypeAdapter<Object> delegate = (TypeAdapter<Object>) optionalValuesDelegators.get(innerTt);
					
					// TODO : Optionals within Optionals :/
					
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