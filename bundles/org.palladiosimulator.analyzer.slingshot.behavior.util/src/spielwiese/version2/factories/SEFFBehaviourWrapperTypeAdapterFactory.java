package spielwiese.version2.factories;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorContextHolder;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorWrapper;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Delegate actual Serialization, but resolve looping reference between {@link SeffBehaviorContextHolder} and {@link SeffBehaviorWrapper}.
 * 
 */
public class SEFFBehaviourWrapperTypeAdapterFactory implements TypeAdapterFactory {

	private final Map<String, TypeAdapter<?>> thingTypes;

	/**
	 * 
	 * @param thingTypes
	 */
	public SEFFBehaviourWrapperTypeAdapterFactory(final Map<String, TypeAdapter<?>> thingTypes) {
		this.thingTypes = thingTypes;
	}

	@Override
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (SeffBehaviorWrapper.class.isAssignableFrom(type.getRawType())) {
			final String className = type.getRawType().getSimpleName();
			// TODO: REMOVE
			if (!thingTypes.containsKey(className)) {
				thingTypes.put(className, gson.getDelegateAdapter(this, type)); // skips "this"
			}
			return (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<SeffBehaviorWrapper>) type);
		}
		return null;
	}

	private TypeAdapter<SeffBehaviorWrapper> customizeMyClassAdapter(final Gson gson,
			final TypeToken<SeffBehaviorWrapper> type) {
		final TypeAdapter<SeffBehaviorWrapper> delegate = gson.getDelegateAdapter(this, type);

		return new TypeAdapter<SeffBehaviorWrapper>() {
			@Override
			public void write(final JsonWriter out, final SeffBehaviorWrapper value) throws IOException {
				delegate.write(out, value);
			}

			@Override
			public SeffBehaviorWrapper read(final JsonReader in) throws IOException {
				final SeffBehaviorWrapper contextWrapper = delegate.read(in);

				try {
					final Field contextField = SeffBehaviorWrapper.class.getDeclaredField("context");
					contextField.setAccessible(true);
					final SeffBehaviorContextHolder context = (SeffBehaviorContextHolder) contextField
							.get(contextWrapper);

					if (context != null) {

						final Field behavioursField = SeffBehaviorContextHolder.class.getDeclaredField("behaviors");
						behavioursField.setAccessible(true);
						final List<SeffBehaviorWrapper> behaviors = (List<SeffBehaviorWrapper>) behavioursField
								.get(context);

						if (behaviors.contains(null)) {
							final int index = behaviors.indexOf(null);
							behaviors.set(index, contextWrapper);
						}
						behavioursField.setAccessible(false);
					}
					contextField.setAccessible(false);
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
						| IllegalAccessException e) {
					e.printStackTrace();
					throw new JsonParseException("Resolving looped references failed", e);
				}
				return contextWrapper;
			}
		};
	}
}