package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.configurer;

import java.nio.file.Path;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.palladiosimulator.analyzer.slingshot.common.constants.model.ModelFileTypeConstants;
import org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.tabs.SimulationArchitectureModelsTab;

import de.uka.ipd.sdq.workflow.launchconfig.tabs.TabHelper;

/**
 * This will create a single text field for the tab that is designed especially
 * for inputing file paths. For describing further information of this system,
 * and in order to construct this field, use the {@link TextFieldModel} class
 * that should contain all the necessary information.
 * 
 * @author Julijan Katic
 */
public abstract class SingleTextModelField
        implements RequiredModelConfiguration {

	/** The text field itself. */
	private Text textField;

	/** The model containing information about the text field. */
	private final TextFieldModel textFieldModel;

	private Path pathInformation;

	/**
	 * Instantiates the required model configuration that configures a simple text
	 * field. The information of the text field is contained in textFieldModel.
	 * 
	 * @param textFieldModel
	 */
	public SingleTextModelField(final TextFieldModel textFieldModel) {
		super();
		this.textFieldModel = textFieldModel;
	}

	@Override
	public void extendContainer(final Composite parent,
	        final SimulationArchitectureModelsTab simulationArchitectureModelsTab) {
		textField = simulationArchitectureModelsTab.createFileInputSection(textFieldModel.groupLabel,
		        textFieldModel.dialogTitle, textFieldModel.fileEndingRestriction, parent);
	}

	@Override
	public void initializeForm(final ILaunchConfiguration configuration) throws CoreException {
		textField.setText(configuration.getAttribute(name(),
		        ModelFileTypeConstants.EMPTY_STRING));
	}

	@Override
	public boolean isValid(final ILaunchConfiguration configuration) {
		return TabHelper.validateFilenameExtension(textField.getText(),
		        textFieldModel.fileEndingRestriction);
	}

	@Override
	public void onApply(final ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(name(), textField.getText());
	}

	protected abstract String name();

	/**
	 * A simple POJO class that describes the text field. Instantiate this class by
	 * using {@link TextFieldModel#of}.
	 * 
	 * @author Julijan Katic
	 */
	public static class TextFieldModel {
		private final String groupLabel;
		private final String dialogTitle;
		private final String[] fileEndingRestriction;

		public TextFieldModel(final String groupLabel, final String dialogTitle, final String[] fileEndingRestriction) {
			super();
			this.groupLabel = groupLabel;
			this.dialogTitle = dialogTitle;
			this.fileEndingRestriction = fileEndingRestriction;
		}

		public static TextFieldModel of(final String groupLabel, final String dialogTitle,
		        final String[] fileEndingRestriction) {
			return new TextFieldModel(groupLabel, dialogTitle, fileEndingRestriction);
		}
	}

}
