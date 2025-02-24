/**
 */
package org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.provider;


import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.ResourceLocator;

import org.eclipse.emf.edit.provider.ComposeableAdapterFactory;
import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.ItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.ViewerNotification;

import org.palladiosimulator.experimentautomation.abstractsimulation.provider.AbstractSimulationConfigurationItemProvider;

import org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration;
import org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationToolAdapterPackage;

/**
 * This is the item provider adapter for a {@link org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration} object.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class StateExplorationConfigurationItemProvider extends AbstractSimulationConfigurationItemProvider {
	/**
	 * This constructs an instance from a factory and a notifier.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public StateExplorationConfigurationItemProvider(AdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	/**
	 * This returns the property descriptors for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		if (itemPropertyDescriptors == null) {
			super.getPropertyDescriptors(object);

			addMinStateDurationPropertyDescriptor(object);
			addMaxIterationsPropertyDescriptor(object);
			addSensitivityPropertyDescriptor(object);
			addDoIdleExplorationPropertyDescriptor(object);
			addModeLocationPropertyDescriptor(object);
			addHorizonPropertyDescriptor(object);
			addCostIntervalPropertyDescriptor(object);
			addCostAmountPropertyDescriptor(object);
		}
		return itemPropertyDescriptors;
	}

	/**
	 * This adds a property descriptor for the Min State Duration feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addMinStateDurationPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_StateExplorationConfiguration_minStateDuration_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_StateExplorationConfiguration_minStateDuration_feature", "_UI_StateExplorationConfiguration_type"),
				 StateExplorationToolAdapterPackage.Literals.STATE_EXPLORATION_CONFIGURATION__MIN_STATE_DURATION,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.REAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Max Iterations feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addMaxIterationsPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_StateExplorationConfiguration_maxIterations_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_StateExplorationConfiguration_maxIterations_feature", "_UI_StateExplorationConfiguration_type"),
				 StateExplorationToolAdapterPackage.Literals.STATE_EXPLORATION_CONFIGURATION__MAX_ITERATIONS,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Sensitivity feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addSensitivityPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_StateExplorationConfiguration_sensitivity_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_StateExplorationConfiguration_sensitivity_feature", "_UI_StateExplorationConfiguration_type"),
				 StateExplorationToolAdapterPackage.Literals.STATE_EXPLORATION_CONFIGURATION__SENSITIVITY,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.REAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Do Idle Exploration feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addDoIdleExplorationPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_StateExplorationConfiguration_doIdleExploration_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_StateExplorationConfiguration_doIdleExploration_feature", "_UI_StateExplorationConfiguration_type"),
				 StateExplorationToolAdapterPackage.Literals.STATE_EXPLORATION_CONFIGURATION__DO_IDLE_EXPLORATION,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Mode Location feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addModeLocationPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_StateExplorationConfiguration_modeLocation_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_StateExplorationConfiguration_modeLocation_feature", "_UI_StateExplorationConfiguration_type"),
				 StateExplorationToolAdapterPackage.Literals.STATE_EXPLORATION_CONFIGURATION__MODE_LOCATION,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Horizon feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addHorizonPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_StateExplorationConfiguration_horizon_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_StateExplorationConfiguration_horizon_feature", "_UI_StateExplorationConfiguration_type"),
				 StateExplorationToolAdapterPackage.Literals.STATE_EXPLORATION_CONFIGURATION__HORIZON,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Cost Interval feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addCostIntervalPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_StateExplorationConfiguration_costInterval_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_StateExplorationConfiguration_costInterval_feature", "_UI_StateExplorationConfiguration_type"),
				 StateExplorationToolAdapterPackage.Literals.STATE_EXPLORATION_CONFIGURATION__COST_INTERVAL,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.REAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Cost Amount feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addCostAmountPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_StateExplorationConfiguration_costAmount_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_StateExplorationConfiguration_costAmount_feature", "_UI_StateExplorationConfiguration_type"),
				 StateExplorationToolAdapterPackage.Literals.STATE_EXPLORATION_CONFIGURATION__COST_AMOUNT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.REAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This returns StateExplorationConfiguration.gif.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/StateExplorationConfiguration"));
	}

	/**
	 * This returns the label text for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getText(Object object) {
		String label = ((StateExplorationConfiguration)object).getName();
		return label == null || label.length() == 0 ?
			getString("_UI_StateExplorationConfiguration_type") :
			getString("_UI_StateExplorationConfiguration_type") + " " + label;
	}


	/**
	 * This handles model notifications by calling {@link #updateChildren} to update any cached
	 * children and by creating a viewer notification, which it passes to {@link #fireNotifyChanged}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void notifyChanged(Notification notification) {
		updateChildren(notification);

		switch (notification.getFeatureID(StateExplorationConfiguration.class)) {
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MIN_STATE_DURATION:
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MAX_ITERATIONS:
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__SENSITIVITY:
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__DO_IDLE_EXPLORATION:
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__MODE_LOCATION:
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__HORIZON:
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__COST_INTERVAL:
			case StateExplorationToolAdapterPackage.STATE_EXPLORATION_CONFIGURATION__COST_AMOUNT:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false, true));
				return;
		}
		super.notifyChanged(notification);
	}

	/**
	 * This adds {@link org.eclipse.emf.edit.command.CommandParameter}s describing the children
	 * that can be created under this object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected void collectNewChildDescriptors(Collection<Object> newChildDescriptors, Object object) {
		super.collectNewChildDescriptors(newChildDescriptors, object);
	}

	/**
	 * Return the resource locator for this item provider's resources.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ResourceLocator getResourceLocator() {
		return StateexplorationtooladapterEditPlugin.INSTANCE;
	}

}
