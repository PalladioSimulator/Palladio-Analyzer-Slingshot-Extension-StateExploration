package org.palladiosimulator.analyzer.slingshot.behavior.util.visitors;

import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.ThinkTime;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.UserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.ClosedWorkloadUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserStarted;
import org.palladiosimulator.analyzer.slingshot.behavior.util.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;

public class UserChangedEventVisitor extends SetReferencingVisitor {
	private final Function<DESEvent, DESEvent> visitor;

	public UserChangedEventVisitor(final PCMResourceSetPartition set) {
		super(set);

		this.visitor = new LambdaVisitor<DESEvent, DESEvent>()
				.on(ClosedWorkloadUserInitiated.class).then(this::clone)
				.on(UserFinished.class).then(this::clone)
				.on(UserStarted.class).then(this::clone);
	}

	private DESEvent clone(final ClosedWorkloadUserInitiated clonee) {
		final UserInterpretationContext clonedContext = helper.cloneUserInterpretationContext(clonee.getEntity());
		final double remainingthinktime = clonee.delay();

		final CoreFactory coreFactory = CoreFactory.eINSTANCE;
		final PCMRandomVariable var = coreFactory.createPCMRandomVariable();
		var.setSpecification(String.valueOf(remainingthinktime));

		final ThinkTime newThinktime = new ThinkTime(var);
		return new ClosedWorkloadUserInitiated(clonedContext, newThinktime);
	}

	private DESEvent clone(final UserFinished clonee) {
		return new UserFinished(helper.cloneUserInterpretationContext(clonee.getEntity()));
	}

	private DESEvent clone(final UserStarted clonee) {
		return new UserStarted(helper.cloneUserInterpretationContext(clonee.getEntity()));
	}

	// TODO :
	// UserSlept
	// UserWokeUp
	// InnerScenarioBehaviorInitiated

	public DESEvent visit(final DESEvent e) {
		return this.visitor.apply(e);
	}

	public static DESEvent visit(final DESEvent e, final double offset) {
		return null;
	}
}
