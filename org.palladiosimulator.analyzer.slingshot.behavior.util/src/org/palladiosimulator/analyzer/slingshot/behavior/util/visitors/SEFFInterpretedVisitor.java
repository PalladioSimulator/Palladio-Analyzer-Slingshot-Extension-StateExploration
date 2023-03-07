package org.palladiosimulator.analyzer.slingshot.behavior.util.visitors;

import java.util.function.Function;

import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFChildInterpretationStarted;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFExternalActionCalled;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFInterpretationFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFInterpretationProgressed;
import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelper;
import org.palladiosimulator.analyzer.slingshot.behavior.util.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.simulation.events.DESEvent;

public class SEFFInterpretedVisitor {
	private final Function<DESEvent, DESEvent> cloneFactory;
	private final CloneHelper helper = new CloneHelper();

	public SEFFInterpretedVisitor() {
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

	public DESEvent visit(DESEvent e) {
		return this.cloneFactory.apply(e);
	}
}
