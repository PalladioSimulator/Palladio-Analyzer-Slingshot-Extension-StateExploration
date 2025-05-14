package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.adapters;

import java.io.IOException;

import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.util.Shareables;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ClassTypeAdapter extends TypeAdapter<Class<?>> {

	public ClassTypeAdapter() {
		super();
	}

	@Override
	public void write(final JsonWriter out, final Class<?> value) throws IOException {
		out.value(value.getCanonicalName());
	}

	@Override
	public Class<?> read(final JsonReader in) throws IOException {
		final String s = in.nextString();
		try {
			return Shareables.getClassHelper(s);
		} catch (final ClassNotFoundException e) {
			throw new JsonParseException("Failed to parse" + s + "to java class.", e);
		}
	}
}
