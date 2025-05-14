package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractGenericEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * 
 * Factory to create {@link TypeAdapter}s for {@link DESEvent}s.
 * 
 * @author Sophie Stieß
 * 
 */
public class DESEventTypeAdapterFactory implements TypeAdapterFactory {

	/**
	 * Names for the fields to be serialized into the JSON. Does not match names of
	 * any fields of existing classes.
	 */
	public static final String FIELD_NAME_CLASS = "type";
	public static final String FIELD_NAME_EVENT = "event";

	/**
	 * Name of the field containing the {@link com.google.common.reflect.TypeToken}
	 * in {@link AbstractGenericEvent}. Must be adapted, if the field name ever
	 * changes.
	 */
	public static final String FIELD_NAME_GENERIC_TYPE_TOKEN = "genericTypeToken";

	/**
	 * According to Doc, creating delegators is costly, thus we save them, if
	 * possible.
	 */
	private final Map<TypeToken<?>, TypeAdapter<DESEvent>> eventDelegators = new HashMap<>();

	/**
	 * Instantiate the factory.
	 */
	public DESEventTypeAdapterFactory() {
	}

	@Override
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (DESEvent.class.isAssignableFrom(type.getRawType())) {
			return (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<DESEvent>) type);
		}
		return null;
	}

	private TypeAdapter<DESEvent> customizeMyClassAdapter(final Gson gson, final TypeToken<DESEvent> type) {
		final TypeAdapter<DESEvent> delegate = gson.getDelegateAdapter(this, type); // used for writing only.
		final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

		// final DESEventTypeAdapterFactory forReference = this;

		return new TypeAdapter<DESEvent>() {
			@Override
			public void write(final JsonWriter out, final DESEvent value) throws IOException {
				final JsonObject obj = new JsonObject();
				obj.addProperty(FIELD_NAME_CLASS, value.getClass().getCanonicalName());
				obj.add(FIELD_NAME_EVENT, delegate.toJsonTree(value));
				elementAdapter.write(out, obj);
			}

			@Override
			public DESEvent read(final JsonReader in) throws IOException {

				final JsonElement jsonElement = elementAdapter.read(in);

				if (!jsonElement.isJsonObject()) {
					throw new JsonParseException("Expected JsonObject, but found" + jsonElement);
				}

				final JsonObject parent = jsonElement.getAsJsonObject();

				final JsonPrimitive type = this.getFieldAs(parent, JsonElement::isJsonPrimitive,
						JsonElement::getAsJsonPrimitive, FIELD_NAME_CLASS);
				final JsonObject event = this.getFieldAs(parent, JsonElement::isJsonObject,
						JsonElement::getAsJsonObject, FIELD_NAME_EVENT);

				try {
					final var clazz = Class.forName(type.getAsString());

					final TypeAdapter<DESEvent> adapter = AbstractGenericEvent.class.isAssignableFrom(clazz)
							? createAdapterForGenericType((Class<? extends AbstractGenericEvent<?, ?>>) clazz, event)
							: createSimpleAdapter(clazz);

					final DESEvent desEvent = adapter.fromJsonTree(event);

					return desEvent;

				} catch (final ClassNotFoundException e) {
					throw new JsonParseException("Failed to parse message: " + jsonElement, e);
				}

			}

			/**
			 * Get delegate adapter to deserialise normal {@link DESEvent}s.
			 * 
			 * @param eventClass
			 * @return
			 */
			private TypeAdapter<DESEvent> createSimpleAdapter(final Class<?> eventClass) {
				return this.getDelegatAdapter(TypeToken.get(eventClass));
			}

			/**
			 * Get delegate adapter for the given {@link TypeToken}.
			 * 
			 * All delegators are saved and reused because creating type adapters is
			 * expensive.
			 * 
			 * @param tt the type token
			 * @return delegate adapter for the given type token.
			 */
			private TypeAdapter<DESEvent> getDelegatAdapter(final TypeToken<?> tt) {
				if (!eventDelegators.containsKey(tt)) {
					eventDelegators.putIfAbsent(tt,
							(TypeAdapter<DESEvent>) gson.getDelegateAdapter(DESEventTypeAdapterFactory.this, tt));
				}

				return eventDelegators.get(tt);
			}

			/**
			 * Get delegate adapter to deserialise reified {@link DESEvent}s.
			 * 
			 * Requires the given class to be a subtype of {@link AbstractGenericEvent}.
			 * Requires the given {@link JsonObject} to represent an object that is
			 * assignable to {@link AbstractGenericEvent}.
			 * 
			 * This detour is required because of type erasure. As an example, when
			 * serialising a {@code SEFFModelPassedElement<StartImpl>} we must create a
			 * parameterized type for deserializing, or else the {@code StartImpl} will not be deserialized correctly.
			 * 
			 * 
			 * @param eventClass class for which a type adapter should be created.
			 * @param jsonObj
			 * @return
			 */
			private TypeAdapter<DESEvent> createAdapterForGenericType(
					final Class<? extends AbstractGenericEvent<?, ?>> eventClass, final JsonObject jsonObj) {
				if (!jsonObj.has(FIELD_NAME_GENERIC_TYPE_TOKEN)) {
					throw new JsonParseException("expected Object with field genericTypeToken, but field is missing.");
				}

				final JsonElement genericTT = jsonObj.get(FIELD_NAME_GENERIC_TYPE_TOKEN);

				final com.google.common.reflect.TypeToken<?> typeToken = gson.fromJson(genericTT,
						com.google.common.reflect.TypeToken.class);

				final TypeToken<?> gsonEventTT = TypeToken.getParameterized(eventClass, typeToken.getRawType());

				return this.getDelegatAdapter(gsonEventTT);
			}

			/**
			 * 
			 * Just llexing on how to access the fields.
			 * 
			 * @param <T>
			 * @param json
			 * @param criteria
			 * @param accessor
			 * @param fieldName
			 * @return
			 */
			private <T extends JsonElement> T getFieldAs(final JsonObject json, final Predicate<JsonElement> criteria,
					final Function<JsonElement, T> accessor, final String fieldName) {
				if (!json.has(fieldName) || json.get(fieldName) == null || !criteria.test(json.get(fieldName))) {
					throw new JsonParseException(String.format(
							"Expected JsonObjects with non-null field \"%s\" , but found %s.", fieldName, json));
				}
				return accessor.apply(json.get(fieldName));
			}
		};
	}
}