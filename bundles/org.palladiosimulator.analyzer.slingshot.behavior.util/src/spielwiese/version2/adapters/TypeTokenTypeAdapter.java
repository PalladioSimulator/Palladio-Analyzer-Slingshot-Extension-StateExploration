package spielwiese.version2.adapters;

import java.io.IOException;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class TypeTokenTypeAdapter extends TypeAdapter<TypeToken<?>> {

	public TypeTokenTypeAdapter() {
		super();
	}

	@Override
	public void write(final JsonWriter out, final TypeToken<?> value) throws IOException {
		out.value(value.getRawType().getCanonicalName());
	}

	@Override
	public TypeToken<?> read(final JsonReader in) throws IOException {

		final String s = in.nextString();
		
		try {
			final Class<?> clazz = Class.forName(s);
			return new TypeToken<>() {}.resolveType(Class.forName(s));
		} catch (final ClassNotFoundException e) {
			throw new JsonParseException("cannot map typetoken" + s + " to type token class.", e);
		}
	}

}
