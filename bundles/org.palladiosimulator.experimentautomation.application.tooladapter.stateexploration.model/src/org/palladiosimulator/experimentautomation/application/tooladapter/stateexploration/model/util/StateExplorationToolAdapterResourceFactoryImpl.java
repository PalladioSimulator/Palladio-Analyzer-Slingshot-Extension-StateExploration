/**
 */
package org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.util;

import org.eclipse.emf.common.util.URI;

import org.eclipse.emf.ecore.resource.Resource;

import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;

/**
 * <!-- begin-user-doc -->
 * The <b>Resource Factory</b> associated with the package.
 * <!-- end-user-doc -->
 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.util.StateExplorationToolAdapterResourceImpl
 * @generated
 */
public class StateExplorationToolAdapterResourceFactoryImpl extends ResourceFactoryImpl {
	/**
	 * Creates an instance of the resource factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public StateExplorationToolAdapterResourceFactoryImpl() {
		super();
	}

	/**
	 * Creates an instance of the resource.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Resource createResource(URI uri) {
		Resource result = new StateExplorationToolAdapterResourceImpl(uri);
		return result;
	}

} //StateExplorationToolAdapterResourceFactoryImpl
