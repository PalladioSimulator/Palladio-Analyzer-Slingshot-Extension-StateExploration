package org.palladiosimulator.analyzer.slingshot.behavior.util;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.AbstractJobEvent;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateInitialized;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.ActiveResourceFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.ResourceDemandRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFInterpreted;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.UserRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.UserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.AbstractUserChangedEvent;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.InterArrivalUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserEntryRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserRequestFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.util.visitors.JobEventVisitor;
import org.palladiosimulator.analyzer.slingshot.behavior.util.visitors.ModelElementPassedVisitor;
import org.palladiosimulator.analyzer.slingshot.behavior.util.visitors.SEFFInterpretedVisitor;
import org.palladiosimulator.analyzer.slingshot.behavior.util.visitors.UserChangedEventVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

public class CloneHelperWithVisitor {

	private static final Logger LOGGER = Logger.getLogger(CloneHelperWithVisitor.class);

	private final CloneHelper helper;


	private final Function<DESEvent, DESEvent> cloneFactory;

	public CloneHelperWithVisitor(final PCMResourceSetPartition set) {
		this.helper = new CloneHelper(set);

		cloneFactory = new LambdaVisitor<DESEvent, DESEvent>().
				on(UsageModelPassedElement.class).then(e -> (new ModelElementPassedVisitor(set)).visit(e))
				.on(SEFFModelPassedElement.class).then(e -> (new ModelElementPassedVisitor(set)).visit(e))
				.on(AbstractUserChangedEvent.class).then(e -> (new UserChangedEventVisitor(set)).visit(e))
				.on(SEFFInterpreted.class).then(e -> (new SEFFInterpretedVisitor(set)).visit(e))
				.on(AbstractJobEvent.class).then(e -> (new JobEventVisitor(set)).visit(e))
				.
				on(ResourceDemandRequested.class).then(this::clone).
				on(ActiveResourceFinished.class).then(this::clone).
				on(UserRequestFinished.class).then(this::clone).
				on(UserEntryRequested.class).then(this::clone).
				on(InterArrivalUserInitiated.class).then(this::clone).
				on(SPDAdjustorStateInitialized.class).then(this::clone).
				on(DESEvent.class).then(this::log);
	}

	private DESEvent log(final DESEvent event) {
		LOGGER.info(String.format("Did not clone %s. Missing visitor for type %s.", event.toString(),
				event.getClass().getCanonicalName()));
		return null;
	}

	public DESEvent clone (final DESEvent event) {
		return cloneFactory.apply(event);
	}

	public Set<DESEvent> clone (final Set<DESEvent> events) {
		return events.stream().map(cloneFactory).filter(event -> event != null).collect(Collectors.toSet());
	}

	private DESEvent clone(final InterArrivalUserInitiated clonee) {
		final UserInterpretationContext clonedContext = helper.cloneUserInterpretationContext(clonee.getEntity());
		return new InterArrivalUserInitiated(clonedContext, clonee.delay());
	}

	private DESEvent clone(final ResourceDemandRequested clonee) {
		return new ResourceDemandRequested(helper.clone(clonee.getEntity()), clonee.delay());
	}

	private DESEvent clone(final ActiveResourceFinished clonee) {
		return new ActiveResourceFinished(helper.clone(clonee.getEntity()), clonee.delay());
	}

	private DESEvent clone(final UserEntryRequested clonee) {
		final UserRequest clonedRequest = helper.cloneUserRequest(clonee.getEntity());
		final UserInterpretationContext clonedContext = helper.cloneUserInterpretationContext(clonee.getUserInterpretationContext());
		return new UserEntryRequested(clonedRequest, clonedContext, clonee.delay());
	}

	private DESEvent clone(final UserRequestFinished clonee) {
		final UserRequest clonedRequest = helper.cloneUserRequest(clonee.getEntity());
		final UserInterpretationContext clonedContext = helper.cloneUserInterpretationContext(clonee.getUserContext());
		return new UserRequestFinished(clonedRequest, clonedContext);
	}

	private DESEvent clone(final SPDAdjustorStateInitialized clonee) {
		return new SPDAdjustorStateInitialized(clonee.getStateValues());
	}
}
