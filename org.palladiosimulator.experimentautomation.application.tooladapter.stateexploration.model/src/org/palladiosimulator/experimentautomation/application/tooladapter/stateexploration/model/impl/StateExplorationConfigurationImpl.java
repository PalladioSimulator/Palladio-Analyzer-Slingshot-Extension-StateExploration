/**
 */
package org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.palladiosimulator.experimentautomation.abstractsimulation.impl.AbstractSimulationConfigurationImpl;

import org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration;
import org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>State Exploration Configuration</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl.StateExplorationConfigurationImpl#getMinStateDuration <em>Min State Duration</em>}</li>
 *   <li>{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl.StateExplorationConfigurationImpl#getMaxIterations <em>Max Iterations</em>}</li>
 * </ul>
 *
 * @generated
 */
public class StateExplorationConfigurationImpl extends AbstractSimulationConfigurationImpl implements StateExplorationConfiguration {
	/**
	 * The default value of the '{@link #getMinStateDuration() <em>Min State Duration</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMinStateDuration()
	 * @generated
	 * @ordered
	 */
	protected static final double MIN_STATE_DURATION_EDEFAULT = 0.0;

	/**
	 * The cached value of the '{@link #getMinStateDuration() <em>Min State Duration</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMinStateDuration()
	 * @generated
	 * @ordered
	 */
	protected double minStateDuration = MIN_STATE_DURATION_EDEFAULT;

	/**
	 * The default value of the '{@link #getMaxIterations() <em>Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxIterations()
	 * @generated
	 * @ordered
	 */
	protected static final int MAX_ITERATIONS_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getMaxIterations() <em>Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxIterations()
	 * @generated
	 * @ordered
	 */
	protected int maxIterations = MAX_ITERATIONS_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected StateExplorationConfigurationImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return StateExplorationToolAdapterPackage.Literals.STATE_EXPLORATION_CONFIGURATION;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public double getMinStateDuration() {
		return minStateDuration;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMinStateDuration(double newMinStateDuration) {
		double oldMinStateDuration = minStateDuration;
		minStateDuration = newMinStateDuration;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MIN_STATE_DURATION, oldMinStateDuration, minStateDuration));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getMaxIterations() {
		return maxIterations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMaxIterations(int newMaxIterations) {
		int oldMaxIterations = maxIterations;
		maxIterations = newMaxIterations;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MAX_ITERATIONS, oldMaxIterations, maxIterations));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MIN_STATE_DURATION:
				return getMinStateDuration();
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MAX_ITERATIONS:
				return getMaxIterations();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MIN_STATE_DURATION:
				setMinStateDuration((Double)newValue);
				return;
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MAX_ITERATIONS:
				setMaxIterations((Integer)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MIN_STATE_DURATION:
				setMinStateDuration(MIN_STATE_DURATION_EDEFAULT);
				return;
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MAX_ITERATIONS:
				setMaxIterations(MAX_ITERATIONS_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MIN_STATE_DURATION:
				return minStateDuration != MIN_STATE_DURATION_EDEFAULT;
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MAX_ITERATIONS:
				return maxIterations != MAX_ITERATIONS_EDEFAULT;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (minStateDuration: ");
		result.append(minStateDuration);
		result.append(", maxIterations: ");
		result.append(maxIterations);
		result.append(')');
		return result.toString();
	}

} //StateExplorationConfigurationImpl
