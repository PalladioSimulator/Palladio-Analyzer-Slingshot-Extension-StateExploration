package org.palladiosimulator.analyzer.slingshot.behavior.util.visitors;

import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.util.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;

public class ModelElementPassedVisitor extends SetReferencingVisitor {
	private final Function<DESEvent, DESEvent> jobCloneFactory;

	public ModelElementPassedVisitor(final PCMResourceSetPartition set) {
		super(set);
		jobCloneFactory = new LambdaVisitor<DESEvent, DESEvent>().
				on(UsageModelPassedElement.class).then(this::clone);
	}

	private DESEvent clone(final UsageModelPassedElement<?> event) {
		UsageModelPassedElement<?> clonedEvent = null;

		if (Start.class.isAssignableFrom(event.getGenericType())) {
			clonedEvent = new UsageModelPassedElement<Start>((Start) event.getEntity(),
					helper.cloneUserInterpretationContext(event.getContext()));
			clonedEvent.setTime(event.time());
		}
		if (Stop.class.isAssignableFrom(event.getGenericType())) {
			clonedEvent = new UsageModelPassedElement<Stop>((Stop) event.getEntity(),
					helper.cloneUserInterpretationContext(event.getContext()));
		}
		return clonedEvent;
	}

	public DESEvent visit(final DESEvent e) {
		return this.jobCloneFactory.apply(e);
	}
}
