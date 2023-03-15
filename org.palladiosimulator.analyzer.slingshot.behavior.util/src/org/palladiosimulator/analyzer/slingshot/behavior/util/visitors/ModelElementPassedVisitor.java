package org.palladiosimulator.analyzer.slingshot.behavior.util.visitors;

import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelper;
import org.palladiosimulator.analyzer.slingshot.behavior.util.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;

public class ModelElementPassedVisitor {
    private final Function<DESEvent, DESEvent> jobCloneFactory;

	private final CloneHelper helper = new CloneHelper();

	public ModelElementPassedVisitor() {
		jobCloneFactory = new LambdaVisitor<DESEvent, DESEvent>().
				on(UsageModelPassedElement.class).then(this::clone);
	}

	private DESEvent clone(final UsageModelPassedElement<?> event) {
		if (Start.class.isAssignableFrom(event.getGenericType())) {
//			throw new UnsupportedOperationException("TODO : adapt UsageModelPassed Element to make cloning them work");
			return new UsageModelPassedElement<Start>((Start) event.getEntity(),
					helper.cloneUserInterpretationContext(event.getContext()), event.getOffset());
		}
		if (Stop.class.isAssignableFrom(event.getGenericType())) {
//			throw new UnsupportedOperationException("TODO : adapt UsageModelPassed Element to make cloning them work");
			return new UsageModelPassedElement<Stop>((Stop) event.getEntity(),
					helper.cloneUserInterpretationContext(event.getContext()), event.getOffset());
		}
		// TODO throw new .....
		return null;
	}

	public DESEvent visit(final DESEvent e) {
		return this.jobCloneFactory.apply(e);
	}
}
