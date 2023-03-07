package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Composite;
import org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.tabs.SimulationArchitectureModelsTab;

/**
 * This interface is used in order to extend the configuration launcher system.
 * 
 * @author Julijan Katic
 */
public interface RequiredModelConfiguration {

	/**
	 * Extends the SimulationArchitectureModelsTab container with new elements. This
	 * will be first called in order to create the forms.
	 * 
	 * @param parent                          The parent container onto which the
	 * @param simulationArchitectureModelsTab The instance of the tab itself. This
	 *                                        can be used in order to create certain
	 *                                        elements, such as text field, etc.
	 */
	void extendContainer(final Composite parent,
			final SimulationArchitectureModelsTab simulationArchitectureModelsTab);

	/**
	 * Initializes the form after it was created by
	 * {@link #extendContainer(Composite, SimulationArchitectureModelsTab}.
	 */
	void initializeForm(final ILaunchConfiguration configuration) throws CoreException;

	/**
	 * Checks whether a certain input is valid or not.
	 * 
	 * @param configuration The configuration including the validation.
	 * @return true iff it is valid.
	 */
	boolean isValid(final ILaunchConfiguration configuration);

	/**
	 * @return a non-null, non-empty field name.
	 */
	String fieldName();

	/**
	 * When 'apply' was hit on the configuration tab, then this will called with a
	 * copy of the configuration data.
	 * 
	 * @param configuration a copy of the configuration data.
	 */
	void onApply(final ILaunchConfigurationWorkingCopy configuration);
}
