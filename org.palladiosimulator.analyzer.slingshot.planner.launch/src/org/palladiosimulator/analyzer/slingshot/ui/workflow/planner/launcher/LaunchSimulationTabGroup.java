package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.tabs.SimulationArchitectureModelsTab;
import de.uka.ipd.sdq.codegen.simucontroller.runconfig.SimuComConfigurationTab;
import de.uka.ipd.sdq.codegen.simucontroller.runconfig.SimuConfigurationTab;
import de.uka.ipd.sdq.workflow.launchconfig.tabs.DebugEnabledCommonTab;

/**
 * This creates a launch simulation with tabs. As of now, this creates the
 * {@link SimulationArchitectureModelsTab} along with Eclipse's
 * {@link CommonTab}.
 *
 * @author Julijan Katic
 */
public class LaunchSimulationTabGroup extends AbstractLaunchConfigurationTabGroup {

	public LaunchSimulationTabGroup() {
	}

	@Override
	public void createTabs(final ILaunchConfigurationDialog dialog, final String mode) {

		// Assemble the tab pages:
		final ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new SimulationArchitectureModelsTab(),
				new SimuComConfigurationTab(),
				new SimuConfigurationTab(),
				new DebugEnabledCommonTab()
		};

		this.setTabs(tabs);
	}

}
