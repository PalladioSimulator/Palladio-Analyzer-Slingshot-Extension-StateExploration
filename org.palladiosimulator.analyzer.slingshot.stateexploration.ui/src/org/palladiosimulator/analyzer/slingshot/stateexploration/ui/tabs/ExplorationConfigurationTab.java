package org.palladiosimulator.analyzer.slingshot.stateexploration.ui.tabs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.stateexploration.ui.events.ExplorationConfigTabBuilderStarted;
import org.palladiosimulator.analyzer.slingshot.stateexploration.ui.events.ExplorationConfigTabBuilderStarted.TextField;

/**
 *
 * Tab in the run configuration to set exploration specific configurations.
 *
 * @author stiesssh
 *
 */
public class ExplorationConfigurationTab extends AbstractLaunchConfigurationTab {

	private static final Logger LOGGER = Logger.getLogger(ExplorationConfigurationTab.class);

	private static final String NAME = "Exploration Configurations";
	private static final String ID = "org.palladiosimulator.analyzer.slingshot.explorationconfigurationtab";

	private Iterator<TextField> iterator;
	private final Map<TextField, Text> texts = new HashMap<>();

	private final ModifyListener modifyListener;
	private Composite container;

	public ExplorationConfigurationTab() {
		final SystemDriver systemDriver = Slingshot.getInstance().getSystemDriver();

		final ExplorationConfigTabBuilderStarted event = new ExplorationConfigTabBuilderStarted();
		systemDriver.postEventAndThen(event, () -> {
			iterator = event.iterator();
		});

		this.modifyListener = modifyEvent -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		};
	}

	@Override
	public void createControl(final Composite parent) {
		this.container = new Composite(parent, SWT.NONE);
		setControl(container);
		container.setLayout(new GridLayout());

		final Group group = new Group(this.container, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 2;
		group.setLayout(gridLayout_1);
		group.setText("Exploration Settings");

		this.iterator.forEachRemaining(textField -> {
			final Text text = this.createGroupField(modifyListener, textField, group);

			texts.put(textField, text);
		});
	}

	protected Text createGroupField(final ModifyListener modifyListener, final TextField textField,
			final Group group) {
		final Label timeLabel = new Label(group, SWT.NONE);
		timeLabel.setText(textField.getLabel());

		final Text timeField = new Text(group, SWT.BORDER);
		timeField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		timeField.addModifyListener(modifyListener);

		return timeField;
	}

	@Override
	public void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {

	}

	@Override
	public void initializeFrom(final ILaunchConfiguration configuration) {
		texts.forEach((textField, text) -> {
			try {
				text.setText(configuration.getAttribute(textField.getLabel(), textField.getPromptTitle()));
			} catch (final CoreException e) {
				text.setText(textField.getPromptTitle());
			}
		});
	}

	@Override
	public void performApply(final ILaunchConfigurationWorkingCopy configuration) {
		texts.forEach((textField, text) -> {
			configuration.setAttribute(textField.getLabel(), text.getText());
		});
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public boolean isValid(final ILaunchConfiguration launchConfig) {
		setErrorMessage(null);

		return texts.entrySet().stream()
				.filter(entry -> !entry.getKey().isOptional())
				.filter(entry -> entry.getValue().getText().isEmpty())
				.findFirst()
				.map(entry -> {
					setErrorMessage(entry.getKey().getLabel() + " is missing");
					return false;
				})
				.orElse(true);
	}
}