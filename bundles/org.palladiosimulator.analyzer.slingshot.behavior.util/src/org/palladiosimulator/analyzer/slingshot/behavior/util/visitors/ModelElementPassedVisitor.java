package org.palladiosimulator.analyzer.slingshot.behavior.util.visitors;

import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.util.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.StopAction;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;

public class ModelElementPassedVisitor extends SetReferencingVisitor {
	private final Function<DESEvent, DESEvent> jobCloneFactory;

	public ModelElementPassedVisitor(final PCMResourceSetPartition set) {
		super(set);
		jobCloneFactory = new LambdaVisitor<DESEvent, DESEvent>().
				on(UsageModelPassedElement.class).then(this::clone)
				.on(SEFFModelPassedElement.class).then(this::clone);
	}

	private DESEvent clone(final UsageModelPassedElement<?> event) {
		UsageModelPassedElement<?> clonedEvent = null;

		if (Start.class.isAssignableFrom(event.getGenericType())) {
			clonedEvent = new UsageModelPassedElement<Start>(helper.getMatchingPCMElement((Start) event.getEntity()),
					helper.cloneUserInterpretationContext(event.getContext()));
			clonedEvent.setTime(event.time());
		}
		if (Stop.class.isAssignableFrom(event.getGenericType())) {
			clonedEvent = new UsageModelPassedElement<Stop>(helper.getMatchingPCMElement((Stop) event.getEntity()),
					helper.cloneUserInterpretationContext(event.getContext()));
		}
		return clonedEvent;
	}

	private DESEvent clone(final SEFFModelPassedElement<?> event) {
		SEFFModelPassedElement<?> clonedEvent = null;

		if (StartAction.class.isAssignableFrom(event.getGenericType())) {
			clonedEvent = new SEFFModelPassedElement<StartAction>(
					helper.getMatchingPCMElement((StartAction) event.getEntity()),
					helper.cloneContext(event.getContext()));
			clonedEvent.setTime(event.time());
		}
		if (StopAction.class.isAssignableFrom(event.getGenericType())) {
			clonedEvent = new SEFFModelPassedElement<StopAction>(
					helper.getMatchingPCMElement((StopAction) event.getEntity()),
					helper.cloneContext(event.getContext()));
		}
		return clonedEvent;
	}

	public DESEvent visit(final DESEvent e) {
		return this.jobCloneFactory.apply(e);
	}
}
