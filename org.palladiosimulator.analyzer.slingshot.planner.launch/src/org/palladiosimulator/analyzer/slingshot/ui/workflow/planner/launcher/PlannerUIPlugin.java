package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;

public class PlannerUIPlugin extends Plugin implements BundleActivator {

	private static PlannerUIPlugin instance = null;

	// Activate the workflow as well
	private PlannerWorkflowPlugin workflowPlugin = null;

	private Slingshot slingshot = null;

	@Override
	public void start(final BundleContext context) throws Exception {
		instance = this;
		slingshot = Slingshot.getInstance();
		workflowPlugin = PlannerWorkflowPlugin.getInstance();
		super.start(context);
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		instance = null;
		slingshot = null;
		super.stop(context);
	}



}
