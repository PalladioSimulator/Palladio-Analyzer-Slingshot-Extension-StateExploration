package org.palladiosimulator.analyzer.slingshot.stateexploration.ui;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.ExplorationWorkflowPlugin;

public class ExplorationUIPlugin extends Plugin implements BundleActivator {

	private static ExplorationUIPlugin instance = null;

	// Activate the workflow as well
	private ExplorationWorkflowPlugin workflowPlugin = null;

	private Slingshot slingshot = null;

	@Override
	public void start(final BundleContext context) throws Exception {
		instance = this;
		slingshot = Slingshot.getInstance();
		workflowPlugin = ExplorationWorkflowPlugin.getInstance();
		super.start(context);
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		instance = null;
		slingshot = null;
		super.stop(context);
	}



}
