package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.adapters;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.commons.emfutils.EMFLoadHelper;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class EObjectTypeAdapter extends TypeAdapter<EObject> {

	private final ResourceSet set;
	
	public EObjectTypeAdapter(final ResourceSet set) {
		super();
		this.set = set;
	}

	@Override
	public void write(final JsonWriter out, final EObject value) throws IOException {
		final URI uri = EcoreUtil.getURI(value);
		out.value(uri.toString());
	}

	@Override
	public EObject read(final JsonReader in) throws IOException {
		final String s = in.nextString();
		return EMFLoadHelper.loadAndResolveEObject(set, s);
	}
}
