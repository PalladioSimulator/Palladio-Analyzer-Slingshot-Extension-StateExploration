/**
 */
package org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

import org.palladiosimulator.experimentautomation.abstractsimulation.AbstractsimulationPackage;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterFactory
 * @model kind="package"
 * @generated
 */
public interface StateExplorationToolAdapterPackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "model";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://palladiosimulator.org/ExperimentAutomation/ToolAdapter/StateExploration/1.0";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "stateexplorationtooladapter";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	StateExplorationToolAdapterPackage eINSTANCE = org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl.StateExplorationToolAdapterPackageImpl.init();

	/**
	 * The meta object id for the '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl.StateExplorationConfigurationImpl <em>State Exploration Configuration</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl.StateExplorationConfigurationImpl
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl.StateExplorationToolAdapterPackageImpl#getStateExplorationConfiguration()
	 * @generated
	 */
	int STATE_EXPLORATION_CONFIGURATION = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION__NAME = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION__NAME;

	/**
	 * The feature id for the '<em><b>Stop Conditions</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION__STOP_CONDITIONS = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION__STOP_CONDITIONS;

	/**
	 * The feature id for the '<em><b>Random Number Generator Seed</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION__RANDOM_NUMBER_GENERATOR_SEED = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION__RANDOM_NUMBER_GENERATOR_SEED;

	/**
	 * The feature id for the '<em><b>Simulate Linking Resources</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION__SIMULATE_LINKING_RESOURCES = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION__SIMULATE_LINKING_RESOURCES;

	/**
	 * The feature id for the '<em><b>Simulate Failures</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION__SIMULATE_FAILURES = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION__SIMULATE_FAILURES;

	/**
	 * The feature id for the '<em><b>Datasource</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION__DATASOURCE = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION__DATASOURCE;

	/**
	 * The feature id for the '<em><b>Min State Duration</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION__MIN_STATE_DURATION = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Max Iterations</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION__MAX_ITERATIONS = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Sensitivity</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION__SENSITIVITY = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Do Idle Exploration</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION__DO_IDLE_EXPLORATION = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Mode Location</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION__MODE_LOCATION = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION_FEATURE_COUNT + 4;

	/**
	 * The number of structural features of the '<em>State Exploration Configuration</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int STATE_EXPLORATION_CONFIGURATION_FEATURE_COUNT = AbstractsimulationPackage.ABSTRACT_SIMULATION_CONFIGURATION_FEATURE_COUNT + 5;


	/**
	 * Returns the meta object for class '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration <em>State Exploration Configuration</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>State Exploration Configuration</em>'.
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration
	 * @generated
	 */
	EClass getStateExplorationConfiguration();

	/**
	 * Returns the meta object for the attribute '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getMinStateDuration <em>Min State Duration</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Min State Duration</em>'.
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getMinStateDuration()
	 * @see #getStateExplorationConfiguration()
	 * @generated
	 */
	EAttribute getStateExplorationConfiguration_MinStateDuration();

	/**
	 * Returns the meta object for the attribute '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getMaxIterations <em>Max Iterations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Iterations</em>'.
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getMaxIterations()
	 * @see #getStateExplorationConfiguration()
	 * @generated
	 */
	EAttribute getStateExplorationConfiguration_MaxIterations();

	/**
	 * Returns the meta object for the attribute '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getSensitivity <em>Sensitivity</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Sensitivity</em>'.
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getSensitivity()
	 * @see #getStateExplorationConfiguration()
	 * @generated
	 */
	EAttribute getStateExplorationConfiguration_Sensitivity();

	/**
	 * Returns the meta object for the attribute '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#isDoIdleExploration <em>Do Idle Exploration</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Do Idle Exploration</em>'.
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#isDoIdleExploration()
	 * @see #getStateExplorationConfiguration()
	 * @generated
	 */
	EAttribute getStateExplorationConfiguration_DoIdleExploration();

	/**
	 * Returns the meta object for the attribute '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getModeLocation <em>Mode Location</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Mode Location</em>'.
	 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration#getModeLocation()
	 * @see #getStateExplorationConfiguration()
	 * @generated
	 */
	EAttribute getStateExplorationConfiguration_ModeLocation();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	StateExplorationToolAdapterFactory getStateExplorationToolAdapterFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl.StateExplorationConfigurationImpl <em>State Exploration Configuration</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl.StateExplorationConfigurationImpl
		 * @see org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.impl.StateExplorationToolAdapterPackageImpl#getStateExplorationConfiguration()
		 * @generated
		 */
		EClass STATE_EXPLORATION_CONFIGURATION = eINSTANCE.getStateExplorationConfiguration();

		/**
		 * The meta object literal for the '<em><b>Min State Duration</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute STATE_EXPLORATION_CONFIGURATION__MIN_STATE_DURATION = eINSTANCE.getStateExplorationConfiguration_MinStateDuration();

		/**
		 * The meta object literal for the '<em><b>Max Iterations</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute STATE_EXPLORATION_CONFIGURATION__MAX_ITERATIONS = eINSTANCE.getStateExplorationConfiguration_MaxIterations();

		/**
		 * The meta object literal for the '<em><b>Sensitivity</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute STATE_EXPLORATION_CONFIGURATION__SENSITIVITY = eINSTANCE.getStateExplorationConfiguration_Sensitivity();

		/**
		 * The meta object literal for the '<em><b>Do Idle Exploration</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute STATE_EXPLORATION_CONFIGURATION__DO_IDLE_EXPLORATION = eINSTANCE.getStateExplorationConfiguration_DoIdleExploration();

		/**
		 * The meta object literal for the '<em><b>Mode Location</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute STATE_EXPLORATION_CONFIGURATION__MODE_LOCATION = eINSTANCE.getStateExplorationConfiguration_ModeLocation();

	}

} //StateExplorationToolAdapterPackage
