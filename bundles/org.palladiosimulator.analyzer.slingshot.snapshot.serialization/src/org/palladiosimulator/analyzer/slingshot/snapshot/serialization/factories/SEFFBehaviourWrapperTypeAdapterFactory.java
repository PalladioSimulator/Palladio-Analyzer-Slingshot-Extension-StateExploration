package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
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
 * Factory to create {@link TypeAdapter}s for {@link SeffBehaviorWrapper}.
 * 
 * The adapter delegates actual serialization and resolve looping reference
 * between {@link SeffBehaviorWrapper} and {@link SeffBehaviorContextHolder}.
 * 
 * @author Sophie Stieß
 * 
 */
public class SEFFBehaviourWrapperTypeAdapterFactory implements TypeAdapterFactory {

	private static final String FIELD_NAME_BEHAVIORS = "behaviors";
	private static final String FIELD_NANME_CONTEXT = "context";
	
	
	/**
	 * Save all adapters already created with this factory, because i dont see, why
	 * we should create a new adapter every time.
	 */
	private final Map<TypeToken<SeffBehaviorWrapper>, TypeAdapter<SeffBehaviorWrapper>> thisAdapter = new HashMap<>();

	@Override
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		if (SeffBehaviorWrapper.class.isAssignableFrom(type.getRawType())) {
			
			if (!thisAdapter.containsKey(type)) {
				thisAdapter.put((TypeToken<SeffBehaviorWrapper>) type, customizeMyClassAdapter(gson, (TypeToken<SeffBehaviorWrapper>) type));
			}
			
			return (TypeAdapter<T>) thisAdapter.get(type); 
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
					final Field contextField = SeffBehaviorWrapper.class.getDeclaredField(FIELD_NANME_CONTEXT);
					contextField.setAccessible(true);
					final SeffBehaviorContextHolder context = (SeffBehaviorContextHolder) contextField
							.get(contextWrapper);

					if (context != null) {

						final Field behavioursField = SeffBehaviorContextHolder.class.getDeclaredField(FIELD_NAME_BEHAVIORS);
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