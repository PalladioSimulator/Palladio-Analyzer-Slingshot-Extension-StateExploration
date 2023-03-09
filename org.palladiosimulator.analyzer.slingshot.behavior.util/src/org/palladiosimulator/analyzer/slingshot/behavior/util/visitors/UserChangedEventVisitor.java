package org.palladiosimulator.analyzer.slingshot.behavior.util.visitors;

import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.ThinkTime;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.UserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.ClosedWorkloadUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserStarted;
import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelper;
import org.palladiosimulator.analyzer.slingshot.behavior.util.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;

public class UserChangedEventVisitor {
	private final Function<DESEvent, DESEvent> jobCloneFactory;
	private final CloneHelper helper = new CloneHelper();

	public UserChangedEventVisitor() {
		jobCloneFactory = new LambdaVisitor<DESEvent, DESEvent>()
				.on(ClosedWorkloadUserInitiated.class).then(this::clone)
				.on(UserFinished.class).then(this::clone)
				.on(UserStarted.class).then(this::clone);
	}

	private DESEvent clone(final ClosedWorkloadUserInitiated clonee) {
		final UserInterpretationContext clonedContext = helper.cloneUserInterpretationContext(clonee.getEntity());
		final double remainingthinktime = clonee.time();

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
		return this.jobCloneFactory.apply(e);
	}

	public static DESEvent visit(final DESEvent e, final double offset) {
		return null;
	}
}
