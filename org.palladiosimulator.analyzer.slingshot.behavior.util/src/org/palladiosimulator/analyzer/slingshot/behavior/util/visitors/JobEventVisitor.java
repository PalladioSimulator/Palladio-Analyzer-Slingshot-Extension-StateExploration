package org.palladiosimulator.analyzer.slingshot.behavior.util.visitors;

import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.util.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

/**
 * We drop all but the JobInitiated Events, because we must send them again anyway.
 * Other wise we gotta sort the Initiated events out later on, or we spawn additional users...
 *
 * @author stiesssh
 *
 */
public class JobEventVisitor extends SetReferencingVisitor {

	private final Function<DESEvent, DESEvent> jobCloneFactory;

	public JobEventVisitor(final PCMResourceSetPartition set) {
		super(set);

		jobCloneFactory = new LambdaVisitor<DESEvent, DESEvent>().
				on(JobInitiated.class).then(this::clone);
		//on(JobFinished.class).then(this::clone).
		//on(JobProgressed.class).then(this::clone).
		//on(ProcessorSharingJobProgressed.class).then(this::clone);
	}

	private DESEvent clone(final JobInitiated clonee) {
		return new JobInitiated(helper.clone(clonee.getEntity()));
	}

	//	private DESEvent clone(final JobFinished clonee) {
	//		return new JobFinished(helper.clone(clonee.getEntity()));
	//	}

	//	private DESEvent clone(final JobProgressed clonee) {
	//		return new JobProgressed(helper.clone(clonee.getEntity()), );
	//	}
	//
	//	private DESEvent clone(final ProcessorSharingJobProgressed clonee) {
	//		final Job clonedJob = helper.clone(clonee.getEntity());
	//		return new ProcessorSharingJobProgressed(clonedJob, clonee.getDelay(), clonee.getExpectedState());
	//	}

	public DESEvent visit(final DESEvent e) {
		return this.jobCloneFactory.apply(e);
	}
}
