/**
 */
package org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.presentation;

import de.uka.ipd.sdq.identifier.provider.IdentifierEditPlugin;

import de.uka.ipd.sdq.probfunction.provider.ProbabilityFunctionEditPlugin;

import de.uka.ipd.sdq.stoex.provider.StoexEditPlugin;

import de.uka.ipd.sdq.units.provider.UnitsEditPlugin;

import org.eclipse.emf.common.EMFPlugin;

import org.eclipse.emf.common.ui.EclipseUIPlugin;

import org.eclipse.emf.common.util.ResourceLocator;

import org.eclipse.emf.ecore.provider.EcoreEditPlugin;

import org.palladiosimulator.edp2.models.ExperimentData.provider.EDP2EditPlugin;

import org.palladiosimulator.experimentautomation.variation.provider.ExperimentAutomationEditPlugin;

import org.palladiosimulator.metricspec.provider.MetricSpecEditPlugin;

import org.palladiosimulator.monitorrepository.provider.MonitorrepositoryEditPlugin;

import org.palladiosimulator.pcm.core.provider.PalladioComponentModelEditPlugin;

import org.palladiosimulator.semanticspd.provider.SemanticEditPlugin;

import org.palladiosimulator.servicelevelobjective.provider.ServiceLevelObjectiveEditPlugin;

import org.palladiosimulator.spd.provider.ScalingPolicyDefinitionEditPlugin;

import org.scaledl.usageevolution.provider.UsageevolutionEditPlugin;

import tools.descartes.dlim.provider.DlimEditPlugin;

/**
 * This is the central singleton for the Stateexplorationtooladapter editor plugin.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public final class StateexplorationtooladapterEditorPlugin extends EMFPlugin {
	/**
	 * Keep track of the singleton.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final StateexplorationtooladapterEditorPlugin INSTANCE = new StateexplorationtooladapterEditorPlugin();
	
	/**
	 * Keep track of the singleton.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static Implementation plugin;

	/**
	 * Create the instance.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public StateexplorationtooladapterEditorPlugin() {
		super
			(new ResourceLocator [] {
				DlimEditPlugin.INSTANCE,
				EcoreEditPlugin.INSTANCE,
				EDP2EditPlugin.INSTANCE,
				ExperimentAutomationEditPlugin.INSTANCE,
				IdentifierEditPlugin.INSTANCE,
				MetricSpecEditPlugin.INSTANCE,
				MonitorrepositoryEditPlugin.INSTANCE,
				PalladioComponentModelEditPlugin.INSTANCE,
				ProbabilityFunctionEditPlugin.INSTANCE,
				ScalingPolicyDefinitionEditPlugin.INSTANCE,
				SemanticEditPlugin.INSTANCE,
				ServiceLevelObjectiveEditPlugin.INSTANCE,
				StoexEditPlugin.INSTANCE,
				UnitsEditPlugin.INSTANCE,
				UsageevolutionEditPlugin.INSTANCE,
			});
	}

	/**
	 * Returns the singleton instance of the Eclipse plugin.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the singleton instance.
	 * @generated
	 */
	@Override
	public ResourceLocator getPluginResourceLocator() {
		return plugin;
	}
	
	/**
	 * Returns the singleton instance of the Eclipse plugin.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the singleton instance.
	 * @generated
	 */
	public static Implementation getPlugin() {
		return plugin;
	}
	
	/**
	 * The actual implementation of the Eclipse <b>Plugin</b>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static class Implementation extends EclipseUIPlugin {
		/**
		 * Creates an instance.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		public Implementation() {
			super();
	
			// Remember the static instance.
			//
			plugin = this;
		}
	}

}
