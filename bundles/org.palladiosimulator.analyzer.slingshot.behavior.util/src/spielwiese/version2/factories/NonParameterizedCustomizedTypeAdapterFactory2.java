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
 */
public class NonParameterizedCustomizedTypeAdapterFactory2 implements TypeAdapterFactory {

	public static final String FIELD_NAME_CLASS = "class";
	public static final String FIELD_NAME_ID_FOR_REFERENCE = "refId";

	
	private final Set<Class<?>> customizedClasses;
	
	private final Map<String, Object> done;
	private final Map<String, TypeAdapter<?>> thingTypes;
	
	final Set<String> alreadyJsoned = new HashSet<>(); 

	/**
	 * 
	 * @param done
	 * @param thingTypes
	 */
	public NonParameterizedCustomizedTypeAdapterFactory2(final Set<Class<?>> customizables, final Map<String, Object> done, final Map<String, TypeAdapter<?>> thingTypes) {
		this.done = done;
		this.thingTypes = thingTypes;
		this.customizedClasses = customizables;
	}

	@Override
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		for (final Class<?> clazz : customizedClasses) {
			if (clazz.isAssignableFrom(type.getRawType())) {
				
				final String className = type.getRawType().getSimpleName();
				if (!thingTypes.containsKey(className)) {
					thingTypes.put(className, gson.getDelegateAdapter(this, type)); // skips "this" 
				}
				
				return customizeMyClassAdapter(gson, type);
			}
		}
		return null;
	}

	private <R> TypeAdapter<R> customizeMyClassAdapter(final Gson gson, final TypeToken<R> type) {
		final TypeAdapter<R> delegate = gson.getDelegateAdapter(this, type);
		final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
				
		return new TypeAdapter<R>() {
			@Override
			public void write(final JsonWriter out, final R value) throws IOException {
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
					alreadyJsoned.add(refId);
					final JsonObject obj = new JsonObject();

					obj.addProperty(FIELD_NAME_CLASS, value.getClass().getSimpleName());
					obj.addProperty(FIELD_NAME_ID_FOR_REFERENCE, refId);
					
					obj.add("obj", delegate.toJsonTree(value));
					

					elementAdapter.write(out, obj);
				}
			}

			@Override
			public R read(final JsonReader in) throws IOException {
				final JsonElement tree = elementAdapter.read(in);
				if (!tree.isJsonObject() && done.containsKey(tree.getAsString()) ) {
					return (R) done.get(tree.getAsString());
				}
				if (!tree.isJsonObject() && !done.containsKey(tree.getAsString()) ) {
					return null;
				}
				final JsonObject jsonObj = tree.getAsJsonObject();
				final String id = jsonObj.get(FIELD_NAME_ID_FOR_REFERENCE).getAsString();
				final String tt = jsonObj.get(FIELD_NAME_CLASS).getAsString();
				
				
				final R element = (R) thingTypes.get(tt).fromJsonTree(jsonObj.get("obj"));
				done.put(id, element);
				 
				return element;
			}
		};
	}
}