package spielwiese.version1;

import java.io.IOException;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

	public CustomizedTypeAdapterFactory(final Class<C> customizedClass, final Map<String, Object> done) {
		this.customizedClass = customizedClass;
		this.done = done;
	}

	@Override
	@SuppressWarnings("unchecked") 
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		return type.getRawType() == customizedClass
				? (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<C>) type)
				: null;
	}

	private TypeAdapter<C> customizeMyClassAdapter(final Gson gson, final TypeToken<C> type) {
		final TypeAdapter<C> delegate = gson.getDelegateAdapter(this, type);
		final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
		return new TypeAdapter<C>() {
			@Override
			public void write(final JsonWriter out, final C value) throws IOException {
				final JsonElement tree = delegate.toJsonTree(value);
				beforeWrite(value, tree);
				elementAdapter.write(out, tree);
			}

			@Override
			public C read(final JsonReader in) throws IOException {
				final JsonElement tree = elementAdapter.read(in);
				if (!tree.isJsonObject() && done.containsKey(tree.getAsString()) ) {
					return (C) done.get(tree.getAsString());
				}
				final JsonObject jsonObj = tree.getAsJsonObject();
				final String id = jsonObj.get("jsonId").getAsString();
				final C element = delegate.fromJsonTree(tree);
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
	}

	/**
	 * Override this to muck with {@code deserialized} before it parsed into the
	 * application type.
	 */
	protected void afterRead(final JsonElement deserialized) {
	}
}