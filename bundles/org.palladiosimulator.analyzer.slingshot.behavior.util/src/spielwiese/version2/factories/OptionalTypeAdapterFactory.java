package spielwiese.version2.factories;

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
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;


/**
 * 
 * @author https://stackoverflow.com/questions/11271375/gson-custom-seralizer-for-one-variable-of-many-in-an-object-using-typeadapter
 * 
 * 
 * @param <C>
 */
public class OptionalTypeAdapterFactory implements TypeAdapterFactory {
	private final Map<String, Object> done;
	
	private static final String VALUE_FIELD = "value";
	
	private final Map<String, TypeAdapter<?>> thingTypes;
	private final Map<String, TypeAdapter<?>> optionalValuesDelegators = new HashMap<>();

	public OptionalTypeAdapterFactory(final Map<String, Object> done, final Map<String, TypeAdapter<?>> thingTypes) {
		this.done = done;
		this.thingTypes = thingTypes;
	}

	@Override
	@SuppressWarnings("unchecked") 
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (type.getRawType() == Optional.class) {
			final TypeAdapter<T> ta = (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<Class<Optional<? extends Object>>>) type);
			return ta;
		}
		return null;
	}

	private TypeAdapter<Optional<?>> customizeMyClassAdapter(final Gson gson, final TypeToken<Class<Optional<? extends Object>>> type) {
		final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
		
		final OptionalTypeAdapterFactory forReference = this;
		
		final Set<String> alreadyJsoned = new HashSet<>(); 
		
		return new TypeAdapter<Optional<?>>() {
			@Override
			public void write(final JsonWriter out, final Optional<? extends Object> value) throws IOException {
				if (value == null) {
					elementAdapter.write(out, null);
					return;
				}

				final String refId = String.valueOf(value.hashCode());
				if (alreadyJsoned.contains(refId)) {
					elementAdapter.write(out, new JsonPrimitive(refId));
				} else {

					final JsonObject obj = new JsonObject();

					obj.addProperty("class", value.getClass().getSimpleName());
					obj.addProperty("refId", refId);
					
					if (value.isPresent()) {
						obj.addProperty("refId", "opt$" + refId);
						
						final String valueClassName = value.get().getClass().getSimpleName();
						
						// cannot create delegator upfront, becaus we do not know actual value up front.
						if (!optionalValuesDelegators.containsKey(valueClassName)) {
							optionalValuesDelegators.put(valueClassName, gson.getDelegateAdapter(forReference, TypeToken.get(value.get().getClass())));
						}
						final TypeAdapter<Object> delegate = (TypeAdapter<Object>) optionalValuesDelegators.get(valueClassName);
						obj.add(VALUE_FIELD, delegate.toJsonTree(value.get()));
						
					} else {
						obj.addProperty("refId", "opt$" + 0);
					}
					
					
					alreadyJsoned.add(refId);

					elementAdapter.write(out, obj);
				}
			}

			@Override
			public Optional<?> read(final JsonReader in) throws IOException {
				final JsonElement tree = elementAdapter.read(in);
				if (!tree.isJsonObject() && done.containsKey(tree.getAsString()) ) {
					return (Optional<?>) done.get(tree.getAsString());
				}
				final JsonObject jsonObj = tree.getAsJsonObject();
				final String id = jsonObj.get("refId").getAsString();
				final String tt = jsonObj.get("class").getAsString();
				
				final Optional<?> element;
				
				if (id.equals("opt$0")) {
					element = Optional.empty();
				} else {
					
					if (jsonObj.get(VALUE_FIELD).isJsonObject()) {

					final String innerTt = jsonObj.get(VALUE_FIELD).getAsJsonObject().get("class").getAsString();
					
					final TypeAdapter<Object> delegate = (TypeAdapter<Object>) optionalValuesDelegators.get(innerTt);
					final Object innerElement2 = delegate.fromJsonTree(jsonObj.get(VALUE_FIELD));
					element = Optional.of(innerElement2);
					
					} else if (jsonObj.get(VALUE_FIELD).isJsonArray()) {
						throw new JsonParseException("Cannot parse array inside Optional, not yet implemented.");
					} else {
						if (jsonObj.get(VALUE_FIELD).getAsString().startsWith("file:")) {						
							element = Optional.of(gson.fromJson(jsonObj.get(VALUE_FIELD), EObject.class));
						} else {
							final TypeAdapter<Object> delegate = (TypeAdapter<Object>) optionalValuesDelegators.values().iterator().next();
							final Object innerElement2 = delegate.fromJsonTree(jsonObj.get(VALUE_FIELD));
							element = Optional.of(innerElement2);
						}
					}
				}
				
				return element;
			}
		};
	}
}