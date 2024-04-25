package org.palladiosimulator.analyzer.slingshot.stateexploration.application;

import org.eclipse.core.runtime.IProgressMonitor;

import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;

public class HelloWorldJob implements IJob {
	private String who = "World";

	public HelloWorldJob(final String who) {
		this.who = who;
	}

	@Override
	public void execute(final IProgressMonitor monitor) throws JobFailedException, UserCanceledException {
		System.out.println("Hello " + who); // job specific processing
	}

	@Override
	public String getName() {
		return "Hello World Job";
	}

	@Override
	public void cleanup(final IProgressMonitor monitor) throws CleanupFailedException {
		// Nothing to clean up after a run.
	}
}