package spielwiese.version2.factories;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
public abstract class CustomizedTypeAdapterFactory<C> implements TypeAdapterFactory {
	private final Class<C> customizedClass;
	
	private final Map<String, Object> done;
	
	private final Map<String, TypeAdapter<?>> thingTypes;

	public CustomizedTypeAdapterFactory(final Class<C> customizedClass, final Map<String, Object> done, final Map<String, TypeAdapter<?>> thingTypes) {
		this.customizedClass = customizedClass;
		this.done = done;
		this.thingTypes = thingTypes;
	}

	@Override
	@SuppressWarnings("unchecked") 
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		return customizedClass.isAssignableFrom(type.getRawType())
				? (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<C>) type)
				: null;
	}

	private TypeAdapter<C> customizeMyClassAdapter(final Gson gson, final TypeToken<C> type) {
		final TypeAdapter<C> delegate = gson.getDelegateAdapter(this, type);
		final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
		
		final CustomizedTypeAdapterFactory<C>  forReference = this;
		
		final Set<String> alreadyJsoned = new HashSet<>(); 
		
		return new TypeAdapter<C>() {
			@Override
			public void write(final JsonWriter out, final C value) throws IOException {
				if (value == null) {
					elementAdapter.write(out, null);
					return;
				}
				
				if (value instanceof String) {
					System.out.println("TODO what about strings?");
					elementAdapter.write(out, new JsonPrimitive((String) value));
				}

				final String refId = String.valueOf(value.hashCode());
				if (alreadyJsoned.contains(refId)) {
					elementAdapter.write(out, new JsonPrimitive(refId));
				} else {

					final JsonObject obj = new JsonObject();
					
					final String className = value.getClass().getSimpleName();
					
					if (!thingTypes.containsKey(className)) {
						thingTypes.put(className, gson.getDelegateAdapter(forReference, TypeToken.get(value.getClass())));
					}

					obj.addProperty("class", value.getClass().getSimpleName());
					obj.addProperty("refId", refId);
					
					obj.add("obj", delegate.toJsonTree(value));
					
					alreadyJsoned.add(refId);

					elementAdapter.write(out, obj);
				}
			}

			@Override
			public C read(final JsonReader in) throws IOException {
				final JsonElement tree = elementAdapter.read(in);
				if (!tree.isJsonObject() && done.containsKey(tree.getAsString()) ) {
					return (C) done.get(tree.getAsString());
				}
				final JsonObject jsonObj = tree.getAsJsonObject();
				final String id = jsonObj.get("refId").getAsString();
				final String tt = jsonObj.get("class").getAsString();
				
				
				final C element = (C) thingTypes.get(tt).fromJsonTree(jsonObj.get("obj"));
				done.put(id, element);

				afterRead(tree);
				 
				return element;
			}
		};
	}

	/**
	 * Override this to muck with {@code toSerialize} before it is written to the
	 * outgoing JSON stream.
	 */
	protected void beforeWrite(final C source, final JsonElement toSerialize) {
//		if (source != null && toSerialize != null) {
//			final JsonObject custom = toSerialize.getAsJsonObject();
//			custom.add("jsonId", new JsonPrimitive(String.valueOf(source.hashCode())));
//		}
	}

	/**
	 * Override this to muck with {@code deserialized} before it parsed into the
	 * application type.
	 */
	protected void afterRead(final JsonElement deserialized) {
//		final JsonObject custom = deserialized.getAsJsonObject();
//		custom.remove("jsonId");
	}
}