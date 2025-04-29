package spielwiese.version2.adapters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class TypeTokenTypeAdapter extends TypeAdapter<TypeToken<?>> {
	

	private final Map<String, TypeToken<?>> typetokens = new HashMap<>();
	
	public TypeTokenTypeAdapter() {
		super();
	}

	@Override
	public void write(final JsonWriter out, final TypeToken<?> value) throws IOException {
		if (!typetokens.containsKey(value.getRawType().getCanonicalName())) {
			typetokens.put(value.getRawType().getCanonicalName(), value);
		}
		
		out.value(value.getRawType().getCanonicalName());
	}

	@Override
	public TypeToken<?> read(final JsonReader in) throws IOException {
		final String s = in.nextString();
		
		if (typetokens.containsKey(s)) {
			return typetokens.get(s);
		}
		
		throw new JsonParseException("canno map typetoken" + s + "to type token class.");
	}

}
