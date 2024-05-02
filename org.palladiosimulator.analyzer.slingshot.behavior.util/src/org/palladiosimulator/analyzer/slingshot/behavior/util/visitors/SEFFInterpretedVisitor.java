package org.palladiosimulator.analyzer.slingshot.behavior.util.visitors;

import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFChildInterpretationStarted;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFExternalActionCalled;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFInterpretationFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFInterpretationProgressed;
import org.palladiosimulator.analyzer.slingshot.behavior.util.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

public class SEFFInterpretedVisitor extends SetReferencingVisitor {
	private final Function<DESEvent, DESEvent> cloneFactory;

	public SEFFInterpretedVisitor(final PCMResourceSetPartition set) {
		super(set);
		cloneFactory = new LambdaVisitor<DESEvent, DESEvent>()
				.on(SEFFInterpretationProgressed.class).then(this::clone)
				.on(SEFFInterpretationFinished.class).then(this::clone)
				.on(SEFFChildInterpretationStarted.class).then(this::clone)
				.on(SEFFExternalActionCalled.class).then(this::clone);
	}

	private DESEvent clone(final SEFFInterpretationProgressed clonee) {
		return new SEFFInterpretationProgressed(helper.cloneContext(clonee.getEntity()));
	}

	private DESEvent clone(final SEFFInterpretationFinished clonee) {
		return new SEFFInterpretationFinished(helper.cloneContext(clonee.getEntity()));
	}

	private DESEvent clone(final SEFFChildInterpretationStarted clonee) {
		return new SEFFChildInterpretationStarted(helper.cloneContext(clonee.getEntity()));
	}

	private DESEvent clone(final SEFFExternalActionCalled clonee) {
		return new SEFFExternalActionCalled(helper.cloneGeneralEntryRequest(clonee.getEntity()));
	}

	public DESEvent visit(final DESEvent e) {
		return this.cloneFactory.apply(e);
	}
}
