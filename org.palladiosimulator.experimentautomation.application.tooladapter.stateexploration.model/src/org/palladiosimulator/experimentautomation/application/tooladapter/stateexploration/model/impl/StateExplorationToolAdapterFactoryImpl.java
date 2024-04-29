/**
 */
package org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

import org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class StateExplorationToolAdapterFactoryImpl extends EFactoryImpl implements StateExplorationToolAdapterFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static StateExplorationToolAdapterFactory init() {
		try {
			StateExplorationToolAdapterFactory theStateExplorationToolAdapterFactory = (StateExplorationToolAdapterFactory)EPackage.Registry.INSTANCE.getEFactory(StateExplorationToolAdapterPackage.eNS_URI);
			if (theStateExplorationToolAdapterFactory != null) {
				return theStateExplorationToolAdapterFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new StateExplorationToolAdapterFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public StateExplorationToolAdapterFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION: return createStateExplorationConfiguration();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public StateExplorationConfiguration createStateExplorationConfiguration() {
		StateExplorationConfigurationImpl stateExplorationConfiguration = new StateExplorationConfigurationImpl();
		return stateExplorationConfiguration;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public StateExplorationToolAdapterPackage getStateExplorationToolAdapterPackage() {
		return (StateExplorationToolAdapterPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static StateExplorationToolAdapterPackage getPackage() {
		return StateExplorationToolAdapterPackage.eINSTANCE;
	}

} //StateExplorationToolAdapterFactoryImpl
