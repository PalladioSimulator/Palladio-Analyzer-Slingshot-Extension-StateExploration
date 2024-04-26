/**
 */
package org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model;

import org.eclipse.emf.ecore.EObject;

import org.palladiosimulator.experimentautomation.abstractsimulation.AbstractSimulationConfiguration;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>State Exploration Configuration</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getMinStateDuration <em>Min State Duration</em>}</li>
 *   <li>{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getMaxIterations <em>Max Iterations</em>}</li>
 * </ul>
 *
 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterPackage#getStateExplorationConfiguration()
 * @model
 * @generated
 */
public interface StateExplorationConfiguration extends EObject, AbstractSimulationConfiguration {
	/**
	 * Returns the value of the '<em><b>Min State Duration</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Min State Duration</em>' attribute.
	 * @see #setMinStateDuration(double)
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterPackage#getStateExplorationConfiguration_MinStateDuration()
	 * @model required="true"
	 * @generated
	 */
	double getMinStateDuration();

	/**
	 * Sets the value of the '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getMinStateDuration <em>Min State Duration</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Min State Duration</em>' attribute.
	 * @see #getMinStateDuration()
	 * @generated
	 */
	void setMinStateDuration(double value);

	/**
	 * Returns the value of the '<em><b>Max Iterations</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Max Iterations</em>' attribute.
	 * @see #setMaxIterations(int)
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterPackage#getStateExplorationConfiguration_MaxIterations()
	 * @model required="true"
	 * @generated
	 */
	int getMaxIterations();

	/**
	 * Sets the value of the '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getMaxIterations <em>Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Iterations</em>' attribute.
	 * @see #getMaxIterations()
	 * @generated
	 */
	void setMaxIterations(int value);

} // StateExplorationConfiguration
