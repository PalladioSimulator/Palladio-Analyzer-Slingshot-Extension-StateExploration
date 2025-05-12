package spielwiese.version2.adapters;

import java.io.IOException;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ClassTypeAdapter extends TypeAdapter<Class<?>> {
	
	//final Map<String, Class<?>> classes = new HashMap<>();
	
	public ClassTypeAdapter() {
		super();
	}

	@Override
	public void write(final JsonWriter out, final Class<?> value) throws IOException {
//		if (!classes.containsKey(value.getCanonicalName())) {
//			classes.put(value.getCanonicalName(), value);
//		}
		
		out.value(value.getCanonicalName());
	}

	@Override
	public Class<?> read(final JsonReader in) throws IOException {
		final String s = in.nextString();
		
//		if (!classes.containsKey(s)) {
//			try {
//				final Class<?> clazz =  Class.forName(s);
//				classes.put(s, clazz);				
//			} catch (final ClassNotFoundException e) {
//				e.printStackTrace();
//				throw new RuntimeException(e);
//			}			
//		}
//		
//		if (classes.containsKey(s)) {
//			return classes.get(s);
//		}
		
		try {
			return Class.forName(s);
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
			throw new JsonParseException("canno map class" + s + "to java class.", e);
		}
		
		
	}

}
