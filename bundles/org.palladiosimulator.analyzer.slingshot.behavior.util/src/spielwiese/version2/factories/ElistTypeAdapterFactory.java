package spielwiese.version2.factories;

import java.io.IOException;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;


/**
 * 
 * @author Sophie Stieß
 */
public class ElistTypeAdapterFactory implements TypeAdapterFactory {
	
	public ElistTypeAdapterFactory() {
		super();
	}

	@Override
	@SuppressWarnings("unchecked") 
	public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		return EList.class.isAssignableFrom(type.getRawType())
				? (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<EList<? extends EObject>>) type)
				: null;
	}

	private TypeAdapter<EList<EObject>> customizeMyClassAdapter(final Gson gson, final TypeToken<EList<? extends EObject>> type) {		
		final TypeAdapter<EObject> delegate = gson.getDelegateAdapter(this, TypeToken.get(EObject.class));
			
		return new TypeAdapter<EList<EObject>>() {
			@Override
			public void write(final JsonWriter out, final EList<EObject> value) throws IOException {
				out.beginArray();
				for (final EObject obj : value) {
					delegate.write(out, obj);
				}
				
				out.endArray();
			}
			
			@Override
			public EList<EObject> read(final JsonReader in) throws IOException {
				in.beginArray();
				final EList<EObject> rval = new BasicEList<>();

				while (in.peek() == JsonToken.STRING) {
					final EObject o = delegate.read(in);
					rval.add(o);
				}
				
				in.endArray();
				return rval;
			}
		};
	}
}