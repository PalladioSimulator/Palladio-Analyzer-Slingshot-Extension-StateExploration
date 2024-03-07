package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * 
 *
 */
public interface ArchitectureConfiguration extends ModelAccess {

	public ArchitectureConfiguration copy();

	public void transferModelsToSet(final ResourceSet set);

	public URI getUri(final EClass type);

	public String getSegment();

	public ResourceSet getResourceSet();
}
