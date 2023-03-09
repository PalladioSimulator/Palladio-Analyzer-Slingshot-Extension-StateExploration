package org.palladiosimulator.analyzer.slingshot.ui.workflow.planner.launcher;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class PlannerWorkflowPlugin implements BundleActivator {

	private static PlannerWorkflowPlugin instance = null;
	
	@Override
	public void start(BundleContext context) throws Exception {
		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
	}

	public static PlannerWorkflowPlugin getInstance() {
		return instance;
	}
}
