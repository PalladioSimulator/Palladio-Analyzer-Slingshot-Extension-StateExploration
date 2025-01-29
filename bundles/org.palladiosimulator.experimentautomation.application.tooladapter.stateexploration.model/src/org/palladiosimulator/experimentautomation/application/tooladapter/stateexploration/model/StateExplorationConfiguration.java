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
 *   <li>{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getSensitivity <em>Sensitivity</em>}</li>
 *   <li>{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#isDoIdleExploration <em>Do Idle Exploration</em>}</li>
 *   <li>{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getModeLocation <em>Mode Location</em>}</li>
 *   <li>{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getHorizon <em>Horizon</em>}</li>
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

	/**
	 * Returns the value of the '<em><b>Sensitivity</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Sensitivity</em>' attribute.
	 * @see #setSensitivity(double)
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterPackage#getStateExplorationConfiguration_Sensitivity()
	 * @model required="true"
	 * @generated
	 */
	double getSensitivity();

	/**
	 * Sets the value of the '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getSensitivity <em>Sensitivity</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Sensitivity</em>' attribute.
	 * @see #getSensitivity()
	 * @generated
	 */
	void setSensitivity(double value);

	/**
	 * Returns the value of the '<em><b>Do Idle Exploration</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Do Idle Exploration</em>' attribute.
	 * @see #setDoIdleExploration(boolean)
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterPackage#getStateExplorationConfiguration_DoIdleExploration()
	 * @model required="true"
	 * @generated
	 */
	boolean isDoIdleExploration();

	/**
	 * Sets the value of the '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#isDoIdleExploration <em>Do Idle Exploration</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Do Idle Exploration</em>' attribute.
	 * @see #isDoIdleExploration()
	 * @generated
	 */
	void setDoIdleExploration(boolean value);

	/**
	 * Returns the value of the '<em><b>Mode Location</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mode Location</em>' attribute.
	 * @see #setModeLocation(String)
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterPackage#getStateExplorationConfiguration_ModeLocation()
	 * @model
	 * @generated
	 */
	String getModeLocation();

	/**
	 * Sets the value of the '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getModeLocation <em>Mode Location</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Mode Location</em>' attribute.
	 * @see #getModeLocation()
	 * @generated
	 */
	void setModeLocation(String value);

	/**
	 * Returns the value of the '<em><b>Horizon</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Horizon</em>' attribute.
	 * @see #setHorizon(int)
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterPackage#getStateExplorationConfiguration_Horizon()
	 * @model required="true"
	 * @generated
	 */
	int getHorizon();

	/**
	 * Sets the value of the '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getHorizon <em>Horizon</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Horizon</em>' attribute.
	 * @see #getHorizon()
	 * @generated
	 */
	void setHorizon(int value);

} // StateExplorationConfiguration
