package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories;

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
 * 
 * Factory to create {@link TypeAdapter}s for {@link SeffBehaviorContextHolder}.
 * 
 * The adapter delegates actual serialization and resolve looping reference
 * between {@link SeffBehaviorContextHolder} and {@link SeffBehaviorWrapper}.
 * 
 * @author Sophie Stieß
 * 
 */
public class SEFFBehaviourContextHolderTypeAdapterFactory implements TypeAdapterFactory {
	
	private static final String FIELD_NAME_BEHAVIORS = "behaviors";
	private static final String FIELD_NANME_CONTEXT = "context";
	

	private final Map<String, TypeAdapter<?>> thingTypes;

	/**
	 * 
	 * @param thingTypes
	 */
	public SEFFBehaviourContextHolderTypeAdapterFactory(final Map<String, TypeAdapter<?>> thingTypes) {
		this.thingTypes = thingTypes;
	}

	@Override
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (SeffBehaviorContextHolder.class.isAssignableFrom(type.getRawType())) {
			final String className = type.getRawType().getCanonicalName();
			// TODO: REMOVE
			if (!thingTypes.containsKey(className)) {
				thingTypes.put(className, gson.getDelegateAdapter(this, type)); // skips "this"
			}
			return (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<SeffBehaviorContextHolder>) type);
		}
		return null;
	}

	private TypeAdapter<SeffBehaviorContextHolder> customizeMyClassAdapter(final Gson gson,
			final TypeToken<SeffBehaviorContextHolder> type) {
		final TypeAdapter<SeffBehaviorContextHolder> delegate = gson.getDelegateAdapter(this, type);

		return new TypeAdapter<SeffBehaviorContextHolder>() {
			@Override
			public void write(final JsonWriter out, final SeffBehaviorContextHolder value) throws IOException {
				delegate.write(out, value);
			}

			@Override
			public SeffBehaviorContextHolder read(final JsonReader in) throws IOException {
				final SeffBehaviorContextHolder contextHolder = delegate.read(in);

				try {
					final Field behavioursField = SeffBehaviorContextHolder.class.getDeclaredField(FIELD_NAME_BEHAVIORS);
					behavioursField.setAccessible(true);
					final List<SeffBehaviorWrapper> behaviors = (List<SeffBehaviorWrapper>) behavioursField.get(contextHolder);

					for (final SeffBehaviorWrapper wrapper : behaviors) {
						if (wrapper != null) {
							final Field contextField = SeffBehaviorWrapper.class.getDeclaredField(FIELD_NANME_CONTEXT);
							contextField.setAccessible(true);
							contextField.set(wrapper, contextHolder);
							contextField.setAccessible(false);
						}
					}
					
					behavioursField.setAccessible(false);
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
						| IllegalAccessException e) {
					e.printStackTrace();
					throw new JsonParseException("Resolving looped references failed", e);
				}
				return contextHolder;
			}
		};
	}
}