/**
 */
package org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.util;

import java.util.Map;

import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.resource.Resource;

import org.eclipse.emf.ecore.xmi.util.XMLProcessor;

import org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterPackage;

/**
 * This class contains helper methods to serialize and deserialize XML documents
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class StateExplorationToolAdapterXMLProcessor extends XMLProcessor {

	/**
	 * Public constructor to instantiate the helper.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public StateExplorationToolAdapterXMLProcessor() {
		super((EPackage.Registry.INSTANCE));
		StateExplorationToolAdapterPackage.eINSTANCE.eClass();
	}
	
	/**
	 * Register for "*" and "xml" file extensions the StateExplorationToolAdapterResourceFactoryImpl factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected Map<String, Resource.Factory> getRegistrations() {
		if (registrations == null) {
			super.getRegistrations();
			registrations.put(XML_EXTENSION, new StateExplorationToolAdapterResourceFactoryImpl());
			registrations.put(STAR_EXTENSION, new StateExplorationToolAdapterResourceFactoryImpl());
		}
		return registrations;
	}

} //StateExplorationToolAdapterXMLProcessor
