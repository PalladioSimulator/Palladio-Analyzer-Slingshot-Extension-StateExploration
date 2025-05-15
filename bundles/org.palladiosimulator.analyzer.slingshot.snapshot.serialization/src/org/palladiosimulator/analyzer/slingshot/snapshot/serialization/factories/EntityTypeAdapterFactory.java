package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.util.Shareables;

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
 * Factory to create {@link TypeAdapter}s for any slingshot entities.
 * 
 * Notable, the adapters include the entities runtime type into the JSON, and
 * use references if an object is referenced by multiple other objects.
 * 
 * @author Sophie Stieß
 * 
 */
public class EntityTypeAdapterFactory implements TypeAdapterFactory {

	public static final String FIELD_NAME_CLASS = "class";
	public static final String FIELD_NAME_ID_FOR_REFERENCE = "refId";
	public static final String FIELD_NAME_OBJECT = "obj";

	
	private final Set<Class<?>> customizedClasses;
	
	private final Map<String, Object> done;
	private final Map<String, TypeAdapter<?>> delegateAdapters;
	private final Set<Class<?>> classes; 
	
	final Set<String> alreadyJsoned = new HashSet<>(); 

	/**
	 * 
	 * TODO: merge customizables and classes? 
	 * 
	 * @param done
	 * @param delegateAdapters
	 */
	public EntityTypeAdapterFactory(final Set<Class<?>> customizables, final Map<String, Object> done, final Map<String, TypeAdapter<?>> delegateAdapters, final Set<Class<?>> classes) {
		this.done = done;
		this.delegateAdapters = delegateAdapters;
		this.customizedClasses = customizables;
		this.classes = classes;
	}

	@Override
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (delegateAdapters.isEmpty()) {
			initDelegateAdapters(gson, classes);
		}
		for (final Class<?> clazz : customizedClasses) {
			if (clazz.isAssignableFrom(type.getRawType())) {
				return customizeMyClassAdapter(gson, type);
			}
		}
		return null;
	}
	
	private void initDelegateAdapters(final Gson gson, final Set<Class<?>> classes) {		
		for (final Class<?> clazz : classes) {
			delegateAdapters.put(clazz.getCanonicalName(), gson.getDelegateAdapter(this, TypeToken.get(clazz)));			
		}
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

				final String refId = Shareables.getReferenceId(value);
				
				if (alreadyJsoned.contains(refId)) {
					elementAdapter.write(out, new JsonPrimitive(refId));
				} else {
					alreadyJsoned.add(refId);
					final JsonObject obj = new JsonObject();

					obj.addProperty(FIELD_NAME_CLASS, value.getClass().getCanonicalName());
					obj.addProperty(FIELD_NAME_ID_FOR_REFERENCE, refId);
					
					obj.add(FIELD_NAME_OBJECT, delegate.toJsonTree(value));
					

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
				
				if (!jsonObj.has(FIELD_NAME_ID_FOR_REFERENCE)) {
					System.out.println("no reference in" + jsonObj.toString());
				}
				
				final String id = jsonObj.get(FIELD_NAME_ID_FOR_REFERENCE).getAsString();
				final String tt = jsonObj.get(FIELD_NAME_CLASS).getAsString();
				
				if (!delegateAdapters.containsKey(tt)) {
					throw new JsonParseException("Missing Type mapping for " + tt);
				}
				
				final R element = (R) delegateAdapters.get(tt).fromJsonTree(jsonObj.get(FIELD_NAME_OBJECT));
				done.put(id, element);
				 
				return element;
			}
		};
	}
}