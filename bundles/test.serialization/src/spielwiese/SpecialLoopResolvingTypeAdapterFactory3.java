package spielwiese;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import spielwiese.version2.LoopThingChild;
import spielwiese.version2.LoopThingParent;

/**
 * 
 * 
 */
public class SpecialLoopResolvingTypeAdapterFactory3 implements TypeAdapterFactory {

	private final Map<String, TypeAdapter<?>> thingTypes;

	/**
	 * 
	 * @param done
	 * @param thingTypes
	 */
	public SpecialLoopResolvingTypeAdapterFactory3(final Map<String, Object> done,
			final Map<String, TypeAdapter<?>> thingTypes) {
		this.thingTypes = thingTypes;
	}

	@Override
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (LoopThingParent.class.isAssignableFrom(type.getRawType())) {
			final String className = type.getRawType().getCanonicalName();
			if (!thingTypes.containsKey(className)) {
				thingTypes.put(className, gson.getDelegateAdapter(this, type)); // skips "this"
			}
			return (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<LoopThingParent>) type);
		}
		return null;
	}

	private TypeAdapter<LoopThingParent> customizeMyClassAdapter(final Gson gson,
			final TypeToken<LoopThingParent> type) {
		final TypeAdapter<LoopThingParent> delegate = gson.getDelegateAdapter(this, type); 
		// here, we get an Entity adapter, if we register this adapter factory
		// *after* the entity factory

		return new TypeAdapter<LoopThingParent>() {
			@Override
			public void write(final JsonWriter out, final LoopThingParent value) throws IOException {

				delegate.write(out, value);

			}

			@Override
			public LoopThingParent read(final JsonReader in) throws IOException {
				final LoopThingParent element = delegate.read(in);

				for (final LoopThingChild child : element.getChildren()) {
					try {
						final Field f_declared = LoopThingChild.class.getDeclaredField("parent");
						f_declared.setAccessible(true);
						f_declared.set(child, element);
						f_declared.setAccessible(false);
					} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
							| SecurityException e) {
						e.printStackTrace();
						throw new JsonParseException("Failed to set looped references.", e);
					}
				}

				return element;
			}
		};
	}
}