package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * 
 * 
 *
 */
public interface SetBasedArchitectureConfiguration extends ArchitectureConfiguration {

	public List<Resource> getResources();

	public ResourceSet getResourceSet();

	public URI getUri(final EClass type);
	
	public String getSegment();

	public SetBasedArchitectureConfiguration copy();
}
