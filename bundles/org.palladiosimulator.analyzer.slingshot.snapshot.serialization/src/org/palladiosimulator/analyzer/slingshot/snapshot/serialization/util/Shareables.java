package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.util;

import java.util.HashMap;
import java.util.Map;

public class Shareables {

	private final static Map<String, Class<?>> knownClasses = new HashMap<>();

	public static Class<?> getClassHelper(final String s) throws ClassNotFoundException {
		if (!knownClasses.containsKey(s)) {
			final Class<?> clazz = Class.forName(s);
			knownClasses.put(s, clazz);
		}
		return knownClasses.get(s);
	}
	
	public static <R> String getReferenceId(final R value) {
		return String.valueOf(value.hashCode())+"$"+value.getClass().hashCode();
	}

}
