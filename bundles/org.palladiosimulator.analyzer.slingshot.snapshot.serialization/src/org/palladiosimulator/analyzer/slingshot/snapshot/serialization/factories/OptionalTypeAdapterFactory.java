package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;

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
 * Differentiates between three "types" of Optionals:
 * <ul>
 * <li> empty optional: value is marked as "empty" such that empty optionals can be recognized on deserialisation. 
 * <ul><li>{@code {"class":"java.util.Optional","value":"empty"}}</ul>
 * <li> non-empty Optional and the value is an object that was not yet serialised: value is serialised with respective adapter.
 * <ul>
 * <li> object: {@code {"class":"java.util.Optional","value":{"class":"...","refId":"...","obj":{....}}}}
 * <li> PCM element : {@code {"class":"java.util.Optional","value":"file:..."}} (the "file" prefix is important for recognising PCM elements)
 * </ul>
 * <li> non-empty Optional and the value is an object that was already serialised: reference to the object and type of the object.
 * <ul>
 * <li> {"class":"java.util.Optional","value":{"class":"...","ref":"..."}} 
 * <li> the additional inclusion of the value's type is necessary, because otherwise the adapter does not know the target type when reading the reference.
 * </ul>
 * </ul>
 * 	
 * Beware, this adapter CANNOT handle optionals within optionals. 
 * 
 * @author Sophie Stieß
 * 
 */
public class OptionalTypeAdapterFactory implements TypeAdapterFactory {
	
	public static final String OPTIONAL_VALUE_FIELD = "value";
	/** marker for an empty optional*/
	public static final String OPTIONAL_EMPTY = "empty";
	/** must differ from {@link EntityTypeAdapterFactory#FIELD_NAME_ID_FOR_REFERENCE}, or else objects and references will be mixed up.*/ 
	public static final String REFERENCE_FIELD = "ref";
	/** must be equal to from {@link EntityTypeAdapterFactory#FIELD_NAME_CLASS}, or else objects and references cannot be handled in the same way. */ 
	public static final String FIELD_NAME_CLASS = EntityTypeAdapterFactory.FIELD_NAME_CLASS; 

	private final Map<String, TypeAdapter<?>> optionalValuesDelegators = new HashMap<>();
	
	private final Set<TypeToken<?>> innerTypes; 

	public OptionalTypeAdapterFactory(final Set<TypeToken<?>> innerTypes) {
		this.innerTypes = new HashSet<>(innerTypes);
	}
	
	

	@Override
	@SuppressWarnings("unchecked")
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (type.getRawType() == Optional.class) {
			if (optionalValuesDelegators.isEmpty()) {
				this.initDelegatorsMap(gson);
			}
			final TypeAdapter<T> thisAdapter = (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<Class<Optional<? extends Object>>>) type);
			
			return thisAdapter;
		}
		return null;
	}

	private void initDelegatorsMap(final Gson gson) {	
		for (final TypeToken<?> token : innerTypes) {
			optionalValuesDelegators.put(token.getRawType().getCanonicalName(), gson.getDelegateAdapter(this,token));			
		}
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
					final TypeAdapter<Object> delegate = this.getDelegateAdapter(gson, optional.get());
					final JsonElement valueJson =  delegate.toJsonTree(optional.get());
					
					if (valueJson.isJsonObject() || this.isPCMModelReference(valueJson)) {
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

			@Override
			public Optional<?> read(final JsonReader in) throws IOException {
				final JsonElement tree = elementAdapter.read(in);

				final JsonObject jsonObj = tree.getAsJsonObject();
				final JsonElement value = jsonObj.get(OPTIONAL_VALUE_FIELD);

				if (value.isJsonObject()) {
					final String valueType = value.getAsJsonObject().get(FIELD_NAME_CLASS).getAsString();

					if (optionalValuesDelegators.containsKey(valueType)) {
						final TypeAdapter<Object> delegate = (TypeAdapter<Object>) optionalValuesDelegators.get(valueType);

						if (value.getAsJsonObject().has(REFERENCE_FIELD)) {
							return Optional.of(delegate.fromJsonTree(value.getAsJsonObject().get(REFERENCE_FIELD)));
						} else {
							return Optional.of(delegate.fromJsonTree(value));
						}
					} else {
						throw new JsonParseException("Missing delegate for value of type" + valueType + "inside an Optional");
					}
				} else if (value.getAsString().equals(OPTIONAL_EMPTY)) {
					return Optional.empty();
				} else if (this.isPCMModelReference(value)) {
					return Optional.of(gson.fromJson(value, EObject.class));
				} else {
					throw new JsonParseException("Unexpected and unhandled primitive inside Optional: " + value.toString());
				}
			}

			/**
			 * 
			 * @param value
			 * @return true, if {@code value} represents a PCM model file, false otherwise.  
			 */
			private boolean isPCMModelReference(final JsonElement value) {
				return value.getAsString().startsWith("file:");
			}
			
			/**
			 * Get a delegate adapter for the given value object.
			 * 
			 * If no matching adapter exist, a new one is created.
			 * 
			 * @param gson for creating a new delegate adapter.
			 * @param value object to get an delegate adapter for.
			 * @return delegate adapter for {@code value}
			 */
			private TypeAdapter<Object> getDelegateAdapter(final Gson gson, final Object value) {
				if (!optionalValuesDelegators.containsKey(value.getClass().getCanonicalName())) {
					optionalValuesDelegators.put(value.getClass().getCanonicalName(), gson.getDelegateAdapter(OptionalTypeAdapterFactory.this, TypeToken.get(value.getClass())));
				}
				return (TypeAdapter<Object>) optionalValuesDelegators.get(value.getClass().getCanonicalName());
			}
		};
	}
}