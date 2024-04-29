package org.palladiosimulator.analyzer.slingshot.stateexploration.workflow;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ExplorationWorkflowPlugin implements BundleActivator {

	private static ExplorationWorkflowPlugin instance = null;
	
	@Override
	public void start(BundleContext context) throws Exception {
		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
	}

	public static ExplorationWorkflowPlugin getInstance() {
		return instance;
	}
}
