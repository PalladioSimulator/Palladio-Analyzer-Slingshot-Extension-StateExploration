package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher.tabs;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.palladiosimulator.analyzer.slingshot.common.constants.model.ModelFileTypeConstants;
import org.palladiosimulator.analyzer.workflow.runconfig.AbstractConfigurationTab;

import de.uka.ipd.sdq.workflow.launchconfig.tabs.TabHelper;

public class SlingshotSpecificTab extends AbstractConfigurationTab {
	
	private final ModifyListener modifyTextListener = e -> {
		this.setDirty(true);
		this.updateLaunchConfigurationDialog();
	};
	
	private Composite container;
	private Text textField;
	
	private Text snaptime;
	
	public SlingshotSpecificTab() {
		
	}

	@Override
	public void createControl(Composite parent) {
		this.container = new Composite(parent, SWT.NONE);
		this.setControl(this.container);
		this.container.setLayout(new GridLayout());
		
		this.textField = this.createFileInputSection("Event Logger File", "Event Logger File",
				ModelFileTypeConstants.LOG_FILE_EXTENSIONS, container, modifyTextListener);
		
		//this.snaptime = this.createFileInputSection("Snap Time", "Snap Time",
		//		ModelFileTypeConstants.LOG_FILE_EXTENSIONS, container, modifyTextListener);
		
		
        this.snaptime = new Text(container, SWT.BORDER);
        this.snaptime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        this.snaptime.addModifyListener(modifyTextListener);
	}
	
	private Text createFileInputSection(final String groupLabel, final String dialogTitle,
			final String[] fileExtensionRestrictions, final Composite parent, final ModifyListener modifyListener) {
		final Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		TabHelper.createFileInputSection(parent, modifyListener, groupLabel, fileExtensionRestrictions, text, dialogTitle, getShell(), ModelFileTypeConstants.EMPTY_STRING);
	
		return text;
	}
	
	@Override
	public String getId() {
		return "org.palladiosimulator.analyzer.slingshot.ui.workflow.config";
	}

	@Override
	public String getName() {
		return "Slingshot Specific Configuration";
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ModelFileTypeConstants.LOG_FILE, this.textField.getText());
		configuration.setAttribute(ModelFileTypeConstants.SNAPTIME, this.snaptime.getText());
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		final boolean valid = TabHelper.validateFilenameExtension(this.textField.getText(), ModelFileTypeConstants.LOG_FILE_EXTENSIONS);
		
		if (!valid) {
			this.setErrorMessage("Please specify a Log file!");
		}
		
		return valid;
	}

	@Override
	public void initializeFrom(final ILaunchConfiguration configuration) {
		try {
			this.textField.setText(configuration.getAttribute(ModelFileTypeConstants.LOG_FILE, ""));
			this.snaptime.setText(configuration.getAttribute(ModelFileTypeConstants.SNAPTIME, ""));
		} catch (CoreException e) {
			e.printStackTrace(); // TODO
		}
	}
	
	
}
