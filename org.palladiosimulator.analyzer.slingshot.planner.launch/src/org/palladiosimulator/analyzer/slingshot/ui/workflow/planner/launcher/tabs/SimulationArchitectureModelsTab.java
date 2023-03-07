package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.tabs;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.palladiosimulator.analyzer.slingshot.common.constants.model.ModelFileTypeConstants;
import org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer.AllocationModelField;
import org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer.MonitorRepositoryModelField;
import org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer.RequiredModelConfiguration;
import org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer.ScalingPolicyDefinitionModelField;
import org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer.UsageModelField;

import de.uka.ipd.sdq.workflow.launchconfig.ImageRegistryHelper;
import de.uka.ipd.sdq.workflow.launchconfig.LaunchConfigPlugin;
import de.uka.ipd.sdq.workflow.launchconfig.tabs.TabHelper;

/**
 * This builds a single tab that is used to specify which models are going to be
 * loaded for the simulation. Typically, on this tab, only text fields are used
 * for pointing to the architecture model path.
 *
 * <p>
 * In order to put more text fields onto the tab, one has to implement the
 * {@link RequiredModelConfiguration} interface. The order of its calls are then
 * the following:
 *
 * <ol>
 * <li>The fields will be created in {@link #createControl} which in return
 * calls {@link RequiredModelConfiguration#extendContainer}.</li>
 * <li>Then, all the created fields will be initialized in
 * {@link #initializeForm} that calls
 * {@link RequiredModelConfiguration#initializeForm}.</li>
 * <li>Afterwards, when the 'apply' button was clicked, then
 * {@link #performApply} is going to be called, which in return calls
 * {@link RequiredModelConfiguration#onApply}.</li>
 * </ol>
 *
 * @author Julijan Katic
 */
public class SimulationArchitectureModelsTab extends AbstractLaunchConfigurationTab {

	/** The id of this plug-in. */
	public static final String PLUGIN_ID = "org.palladiosimulator.analyzer.slingshot.workflow.planner";
	/** The path to the image file for the tab icon. */
	private static final String FILENAME_TAB_IMAGE_PATH = "icons/filenames_tab.gif";

	/**
	 * The list of all configurers that is used for adding new items onto the tab.
	 */
	private final List<RequiredModelConfiguration> configurers;

	/**
	 * The container of the tab itself.
	 */
	private Composite container;

	/**
	 * A certain listener that is activated when a text is changed.
	 */
	private final ModifyListener modifyTextListener;

	public SimulationArchitectureModelsTab() {
		this.modifyTextListener = e -> {
			SimulationArchitectureModelsTab.this.setDirty(true);
			SimulationArchitectureModelsTab.this.updateLaunchConfigurationDialog();
		};

		// final ModelConfigurerRegister register = new ModelConfigurerRegister();
		// this.configurers = register.getAllProviders();
		this.configurers = List.of(new AllocationModelField(), new UsageModelField(),
				new MonitorRepositoryModelField(), new ScalingPolicyDefinitionModelField());
		//this.configurers = List.of(new AllocationModelField(), new UsageModelField());
	}

	@Override
	public void createControl(final Composite parent) {
		this.container = new Composite(parent, SWT.NONE);
		this.setControl(this.container);
		this.container.setLayout(new GridLayout());

		for (final RequiredModelConfiguration configurer : this.configurers) {
			configurer.extendContainer(this.container, this);
		}

	}

	/**
	 * Creates a new Text field for this tab.
	 *
	 * @param groupLabel                The group label containing the single text
	 *                                  field.
	 * @param dialogTitle               The title of the dialog when selecting a
	 *                                  file.
	 * @param fileExtensionRestrictions The allowed file endings that can be
	 *                                  selected.
	 * @param parent                    The container onto which the text field
	 *                                  should be placed onto.
	 * @param modifyListener            A listener for this field.
	 * @return a non-null text field.
	 */
	public Text createFileInputSection(final String groupLabel, final String dialogTitle,
			final String[] fileExtensionRestrictions, final Composite parent, final ModifyListener modifyListener) {
		final Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		TabHelper.createFileInputSection(parent, modifyListener, groupLabel, fileExtensionRestrictions, text,
				dialogTitle, this.getShell(), ModelFileTypeConstants.EMPTY_STRING);
		return text;
	}

	/**
	 * Creates a new Text field for this tab with the standard listener that updates
	 * the configuration dialog on text change.
	 *
	 * @return a non-null text field.
	 * @see #createFileInputSection(String, String, String[], Composite,
	 *      ModifyListener)
	 */
	public Text createFileInputSection(final String groupLabel, final String dialogTitle,
			final String[] fileExtensionRestrictions, final Composite parent) {
		return this.createFileInputSection(groupLabel, dialogTitle, fileExtensionRestrictions, parent,
				this.modifyTextListener);
	}

	@Override
	public void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {

	}

	@Override
	public void initializeFrom(final ILaunchConfiguration configuration) {
		for (final RequiredModelConfiguration configurer : this.configurers) {
			try {
				configurer.initializeForm(configuration);
			} catch (final CoreException e) {
				LaunchConfigPlugin.errorLogger(this.getName(), configurer.fieldName(), e.getMessage());
			}
		}
	}

	@Override
	public void performApply(final ILaunchConfigurationWorkingCopy configuration) {
		for (final RequiredModelConfiguration configurer : this.configurers) {
			configurer.onApply(configuration);
		}
	}

	@Override
	public boolean isValid(final ILaunchConfiguration launchConfig) {
		this.setErrorMessage(null);

		return this.configurers.stream().allMatch(rmc -> rmc.isValid(launchConfig));
	}

	@Override
	public Image getImage() {
		return ImageRegistryHelper.getTabImage(PLUGIN_ID, FILENAME_TAB_IMAGE_PATH);
	}

	@Override
	public String getName() {
		return "Architecture Model(s)";
	}

	@Override
	public String getId() {
		return "org.palladiosimulator.analyzer.performability.ui.workflow.config.PerformabilityArchitectureModelsTab";
	}
}
