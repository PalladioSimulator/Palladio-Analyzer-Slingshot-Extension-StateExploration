/**
 */
package org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterPackage
 * @generated
 */
public interface StateExplorationToolAdapterFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	StateExplorationToolAdapterFactory eINSTANCE = org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl.StateExplorationToolAdapterFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>State Exploration Configuration</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>State Exploration Configuration</em>'.
	 * @generated
	 */
	StateExplorationConfiguration createStateExplorationConfiguration();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	StateExplorationToolAdapterPackage getStateExplorationToolAdapterPackage();

} //StateExplorationToolAdapterFactory
