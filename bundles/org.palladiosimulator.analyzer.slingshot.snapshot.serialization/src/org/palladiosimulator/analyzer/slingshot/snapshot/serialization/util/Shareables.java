package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.adapters.ClassTypeAdapter;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.adapters.EObjectTypeAdapter;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.adapters.TypeTokenTypeAdapter;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories.DESEventTypeAdapterFactory;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories.ElistTypeAdapterFactory;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories.EntityTypeAdapterFactory;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories.OptionalTypeAdapterFactory;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories.SEFFBehaviourContextHolderTypeAdapterFactory;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories.SEFFBehaviourWrapperTypeAdapterFactory;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;

public final class Shareables {

	private final static Map<String, Class<?>> knownClasses = new HashMap<>();

	/**
	 * Helper for getting classes by their name.
	 * 
	 * Getting a class by name is probably expensive, thus we save all known classes
	 * for later usage.
	 * 
	 * @param className name of the class as string.
	 * @return a {@link Class} object matching the given string.
	 * @throws ClassNotFoundException
	 */
	public static Class<?> getClassHelper(final String className) throws ClassNotFoundException {
		if (!knownClasses.containsKey(className)) {
			final Class<?> clazz = Class.forName(className);
			knownClasses.put(className, clazz);
		}
		return knownClasses.get(className);
	}
	
	/**
	 * Helper for creating the reference if for an object.
	 * 
	 * Different adapters must create and use those reference ids, thus we have one
	 * central operation for creating them, such that the ids are always constructed
	 * the same way.
	 * 
	 * @param <R>
	 * @param value object to create a reference id for.
	 * @return reference id for the given value
	 */
	public static <R> String getReferenceId(final R value) {
		return String.valueOf(value.hashCode())+"$"+value.getClass().hashCode();
	}

	/**
	 * Creates a {@link Gson} object for de/serialising a {@link DESEvent}s.
	 * 
	 * 
	 * @param set resource set for de/serialising {@link EObject}s.
	 * @return {@link Gson} object for de/serialising a {@link DESEvent}s.
	 */
	public static Gson createGsonForSlingshot(final ResourceSet set) {
		

		final Map<String, TypeAdapter<?>> thingTypes = new HashMap<>();
		
		final GsonBuilder adaptereBuilder = new GsonBuilder();

		// register direct adapters.
		adaptereBuilder.registerTypeHierarchyAdapter(EObject.class, new EObjectTypeAdapter(set));
		adaptereBuilder.registerTypeHierarchyAdapter(Class.class, new ClassTypeAdapter());
		adaptereBuilder.registerTypeHierarchyAdapter(TypeToken.class, new TypeTokenTypeAdapter());

		// register special factory
		adaptereBuilder.registerTypeAdapterFactory(new SEFFBehaviourContextHolderTypeAdapterFactory());
		adaptereBuilder.registerTypeAdapterFactory(new SEFFBehaviourWrapperTypeAdapterFactory());

		// register factories
		adaptereBuilder.registerTypeAdapterFactory(
				new EntityTypeAdapterFactory(SlingshotTypeTokenSets.typeSetEntities));

		adaptereBuilder.registerTypeAdapterFactory(new OptionalTypeAdapterFactory(SlingshotTypeTokenSets.typeSetOptionals));
		adaptereBuilder.registerTypeAdapterFactory(new ElistTypeAdapterFactory());
		
		adaptereBuilder.registerTypeAdapterFactory(new DESEventTypeAdapterFactory(SlingshotTypeTokenSets.typeSetDESEvents));

		return adaptereBuilder.create();
	}
	

}
