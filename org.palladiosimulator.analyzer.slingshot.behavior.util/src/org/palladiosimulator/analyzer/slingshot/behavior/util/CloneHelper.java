package org.palladiosimulator.analyzer.slingshot.behavior.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.LinkingJob;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.GeneralEntryRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.resource.CallOverWireRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.resource.ResourceDemandRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.SEFFInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.BranchBehaviorContextHolder;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.RootBehaviorContextHolder;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorContextHolder;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.user.RequestProcessingContext;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.InterArrivalTime;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.ThinkTime;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.User;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.UserRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.ClosedWorkloadUserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.OpenWorkloadUserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.UserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.scenariobehavior.BranchScenarioContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.scenariobehavior.LoopScenarioBehaviorContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.scenariobehavior.RootScenarioContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.scenariobehavior.UsageScenarioBehaviorContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.ClosedWorkloadUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.InterArrivalUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.utils.events.ModelPassedEvent;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.pcm.allocation.AllocationContext;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.parameter.VariableUsage;
import org.palladiosimulator.pcm.repository.OperationProvidedRole;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.pcm.resourceenvironment.LinkingResource;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.seff_performance.ParametricResourceDemand;
import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;
import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;

import de.uka.ipd.sdq.simucomframework.variables.stackframe.SimulatedStackframe;

/**
 * Helper to create deep copies of entities.
 *
 * Also updates the referenced PCM models.
 *
 * @author stiesssh
 *
 */
public final class CloneHelper {

	public static final Logger LOGGER = Logger.getLogger(CloneHelper.class);

	private final PCMResourceSetPartition set;

	private final Map<String, User> alreadyClonedUsers = new HashMap<>();

	public CloneHelper(final PCMResourceSetPartition set) {
		this.set = set;
	}

	/**
	 * TODO on a closer look : what is this does not belong here, iirc o_O
	 *
	 * @param event
	 * @param simulationTime
	 * @return
	 */
	public DESEvent clone(final ClosedWorkloadUserInitiated event, final double simulationTime) {
		final double remainingthinktime = event.time() - simulationTime;

		final CoreFactory coreFactory = CoreFactory.eINSTANCE;
		final PCMRandomVariable var = coreFactory.createPCMRandomVariable();
		var.setSpecification(String.valueOf(remainingthinktime));

		final ThinkTime newThinktime = new ThinkTime(var);
		return new ClosedWorkloadUserInitiated(cloneUserInterpretationContext(event.getEntity()), newThinktime);
	}

	/**
	 * TODO on a closer look : what is this does not belong here, iirc o_O
	 *
	 * @param event
	 * @param simulationTime
	 * @return
	 */
	public DESEvent clone(final InterArrivalUserInitiated event, final double simulationTime) {
		return new InterArrivalUserInitiated(event.getEntity(), event.time() - simulationTime);
	}

	/**
	 * TODO on a closer look : what is this does not belong here, iirc o_O
	 *
	 * @param event
	 * @param simulationTime
	 * @return
	 */
	public DESEvent clone(final UsageModelPassedElement<?> event, final double simulationTime) {

		final Object modelElement = event.getModelElement();
		if (modelElement instanceof Start && event.time() <= simulationTime) {
			final UsageModelPassedElement<Start> clonedEvent = new UsageModelPassedElement<Start>(
					this.getMatchingPCMElement((Start) modelElement),
					cloneUserInterpretationContext(((UsageModelPassedElement<?>) event).getContext()));

			setOffset(event.time(), simulationTime, clonedEvent);
			return clonedEvent;
		}
		return event;
	}

	/**
	 * Clone a {@link SEFFModelPassedElement} event but also calculate the offset
	 * for the next simulation run and safe it in the {@code time} field.
	 *
	 * This is not optimal but still the best option to save the offset for later.
	 *
	 * @param event          event to be cloned
	 * @param simulationTime time to calculate offset
	 * @return clone of given event with offset as time.
	 */
	public DESEvent clone(final SEFFModelPassedElement<?> event, final double simulationTime) {

		final Object modelElement = event.getModelElement();
		if (modelElement instanceof StartAction && event.time() <= simulationTime) {
			final SEFFModelPassedElement<StartAction> clonedEvent = new SEFFModelPassedElement<>(
					this.getMatchingPCMElement((StartAction) modelElement), cloneContext(event.getContext()));

			setOffset(event.time(), simulationTime, clonedEvent);

			return clonedEvent;
		}
		return event;
	}

	/**
	 * Calculate an event's offset with respect to the current simulation time and
	 * save the offset in the event's time attribute.
	 *
	 * If the event was created during the simulation run, the offset is the
	 * difference between current simulation time and the event time. If the event
	 * was created during a previous simulation run, i.e. it already entered this
	 * simulation run with a offset into the past, the new offset is the sum of
	 * simulation time and old offset.
	 *
	 * @param eventTime      publication point in time from previous simulation run
	 * @param simulationTime current time of the simulation
	 * @param clonedEvent    the event to be modified
	 */
	private void setOffset(final double eventTime, final double simulationTime,
			final ModelPassedEvent<?> clonedEvent) {
		if (eventTime < 0) {
			final double offset = -(eventTime - simulationTime);
			clonedEvent.setTime(offset);
		} else {
			final double offset = simulationTime - eventTime;
			clonedEvent.setTime(offset);
		}
	}

	/**
	 *
	 * TODO ??
	 *
	 * @param event
	 * @return
	 */
	public DESEvent clone(final UsageModelPassedElement<?> event) {
		final Start modelElement = this.getMatchingPCMElement((Start) event.getModelElement());
		return new UsageModelPassedElement<Start>(modelElement,
				cloneUserInterpretationContext(((UsageModelPassedElement<?>) event).getContext()));
	}

	/**
	 * TODO ??
	 *
	 * @param event
	 * @return
	 */
	public DESEvent clone(final ClosedWorkloadUserInitiated event) {
		final double remainingthinktime = event.delay();

		final CoreFactory coreFactory = CoreFactory.eINSTANCE;
		final PCMRandomVariable var = coreFactory.createPCMRandomVariable();
		var.setSpecification(String.valueOf(remainingthinktime));

		final ThinkTime newThinktime = new ThinkTime(var);
		return new ClosedWorkloadUserInitiated(cloneUserInterpretationContext(event.getEntity()), newThinktime);
	}

	// Operations for cloning the entities //

	/**
	 *
	 * @param job
	 * @return
	 * @throws CloningFailedException
	 */
	public Job clone(final Job job) {
		if (job instanceof final ActiveJob activejob) {
			return clone(activejob);
		} else if (job instanceof final LinkingJob linkingJob) {
			return clone(linkingJob);
		}
		throw new UnsupportedOperationException(
				String.format("Dont Know how to Clone %s", job.getClass().getSimpleName()));
	}

	/**
	 *
	 * Need not match the {@code ProcessingResourceType} as they are defined in the
	 * {@code ResourceRepository}
	 *
	 * @param job
	 * @return
	 * @throws CloningFailedException
	 */
	public Job clone(final ActiveJob job) {
		final ResourceDemandRequest clonedRequest = clone(job.getRequest());

		final AllocationContext matchedAllocContext = getMatchingPCMElement(job.getAllocationContext());

		final Job clonedJob = ActiveJob.builder().withDemand(job.getDemand()).withId(job.getId())
				.withProcessingResourceType(job.getProcessingResourceType()).withRequest(clonedRequest)
				.withAllocationContext(matchedAllocContext).build();

		return clonedJob;
	}

	/**
	 * TODO
	 *
	 * Does not yet work due to problems with cloning the Call Over Wire Requests.
	 *
	 * @param job
	 * @return
	 * @throws CloningFailedException
	 */
	public Job clone(final LinkingJob job) {
		final CallOverWireRequest clonedRequest = clone(job.getRequest());

		final LinkingResource matchee = getMatchingPCMElement(job.getLinkingResource());

		final Job clonedJob = new LinkingJob(job.getId(), job.getDemand(), matchee, clonedRequest);

		return clonedJob;
	}

	/**
	 * TODO PLAIN Cow.
	 *
	 * @param request
	 * @param user    user from the enclosing {@link SEFFInterpretationContext}
	 * @return
	 */
	public CallOverWireRequest clone(final CallOverWireRequest request) {

		final GeneralEntryRequest clonedEntryRequest = cloneGeneralEntryRequest(request.getEntryRequest());

		final User user = clonedEntryRequest.getUser();

		final SimulatedStackframe<Object> variablesToConsider = user.getStack().currentStackFrame();

		final AssemblyContext from = getMatchingPCMElement(request.getFrom());
		final AssemblyContext to = getMatchingPCMElement(request.getTo());
		final Signature signature = getMatchingPCMElement(request.getSignature());

		final CallOverWireRequest.Builder clonedRequestBuilder = CallOverWireRequest.builder().from(from)
				.to(to).signature(signature).user(user).entryRequest(clonedEntryRequest)
				.variablesToConsider(variablesToConsider);

		if (request.getReplyTo().isPresent()) {
			final CallOverWireRequest clonedReplyTo = clone(request.getReplyTo().get());
			clonedRequestBuilder.replyTo(clonedReplyTo);
		}

		return clonedRequestBuilder.build();
	}

	/**
	 * NESTED
	 *
	 * @param request
	 * @return
	 */
	public CallOverWireRequest clone(final CallOverWireRequest request,
			final SEFFInterpretationContext seffIC_calledFrom) {

		final GeneralEntryRequest clonedEntryRequest = cloneGeneralEntryRequest(request.getEntryRequest(),
				seffIC_calledFrom);

		final User user = clonedEntryRequest.getUser();

		final SimulatedStackframe<Object> variablesToConsider = user.getStack().currentStackFrame();

		final AssemblyContext from = getMatchingPCMElement(request.getFrom());
		final AssemblyContext to = getMatchingPCMElement(request.getTo());
		final Signature signature = getMatchingPCMElement(request.getSignature());

		final CallOverWireRequest.Builder clonedRequestBuilder = CallOverWireRequest.builder().from(from)
				.to(to).signature(signature).user(user).entryRequest(clonedEntryRequest)
				.variablesToConsider(variablesToConsider);

		if (request.getReplyTo().isPresent()) {
			final CallOverWireRequest clonedReplyTo = clone(request.getReplyTo().get());
			clonedRequestBuilder.replyTo(clonedReplyTo);
		}

		return clonedRequestBuilder.build();
	}

	/**
	 *
	 * @param resourceDemandRequest
	 * @return
	 * @throws CloningFailedException
	 */
	public ResourceDemandRequest clone(final ResourceDemandRequest resourceDemandRequest) {

		final SEFFInterpretationContext context = cloneContext(resourceDemandRequest.getSeffInterpretationContext());

		final AssemblyContext assemblyContext = getMatchingPCMElement(resourceDemandRequest.getAssemblyContext());
		final ParametricResourceDemand rd = getMatchingPCMElement(resourceDemandRequest.getParametricResourceDemand());

		final ResourceDemandRequest clonedRequest = ResourceDemandRequest.builder()
				.withResourceType(resourceDemandRequest.getResourceType()).withSeffInterpretationContext(context)
				.withAssemblyContext(assemblyContext)
				.withParametricResourceDemand(rd).build();

		return clonedRequest;
	}

	/**
	 *
	 * @param context
	 * @return
	 * @throws CloningFailedException
	 */
	public SEFFInterpretationContext cloneContext(final SEFFInterpretationContext context) {

		final AssemblyContext assemblyContext = getMatchingPCMElement(context.getAssemblyContext());

		// 1 define all content.
		SEFFInterpretationContext clonedParent = null;
		SeffBehaviorContextHolder clonedSeffBehaviorContextHolder = null;
		RequestProcessingContext clonedRequestProcessingContext = null;
		Optional<SEFFInterpretationContext> clonedCalledFrom = Optional.empty();
		CallOverWireRequest clonedCallOverWireRequest = null;
		SimulatedStackframe<Object> clonedStackFrame = null;

		// 2 clone parent.
		if (context.getParent().isPresent()) {
			clonedParent = cloneContext(context.getParent().get());

			clonedRequestProcessingContext = clonedParent.getRequestProcessingContext();
			clonedSeffBehaviorContextHolder = cloneSeffBehaviorContextHolder(context.getBehaviorContext(),
					clonedParent.getBehaviorContext().getCurrentProcessedBehavior());
			clonedCalledFrom = clonedParent.getCaller();

		} else {
			clonedRequestProcessingContext = cloneRequestProcessingContext(context.getRequestProcessingContext());
			clonedSeffBehaviorContextHolder = cloneSeffBehaviorContextHolder(context.getBehaviorContext(), null);

			if (context.getCaller().isPresent()) {
				clonedCalledFrom = Optional.of(cloneContext(context.getCaller().get()));
				// eigentlich darf ich den nicht einfach neu clonen...
			}
		}

		if (context.getCurrentResultStackframe() != null) {
			// acutally no, the result stackframe seems to be a completely different
			// stackframe, that is not part of the user stack.
			clonedStackFrame = clonedRequestProcessingContext.getUser().getStack().currentStackFrame();
		}

		if (context.getCallOverWireRequest().isPresent()) { // TODO probably like above
			if (clonedCalledFrom.isEmpty()) {
				throw new IllegalStateException(
						"If the Call over wire is present, there must also be a caller (i think)!");
			}
			clonedCallOverWireRequest = clone(context.getCallOverWireRequest().get(), clonedCalledFrom.get());
		}

		final SEFFInterpretationContext clonedContext = SEFFInterpretationContext.builder().withParent(clonedParent)
				.withCallOverWireRequest(clonedCallOverWireRequest).withBehaviorContext(clonedSeffBehaviorContextHolder)
				.withCaller(clonedCalledFrom).withRequestProcessingContext(clonedRequestProcessingContext)
				.withAssemblyContext(assemblyContext).withResultStackframe(clonedStackFrame).build();

		return clonedContext;
	}

	/**
	 * TODO
	 *
	 * @param holder
	 * @return
	 */
	public SeffBehaviorContextHolder cloneSeffBehaviorContextHolder(final SeffBehaviorContextHolder holder,
			final SeffBehaviorWrapper clonedParentWrapper) {

		if (holder instanceof final RootBehaviorContextHolder rootholder) {
			return cloneRootBehaviorContextHolder(rootholder);
		}
		if (holder instanceof final BranchBehaviorContextHolder branchholder) {
			return cloneBranchBehaviorContextHolder(branchholder, clonedParentWrapper);
		}
		throw new IllegalArgumentException(
				String.format("Cloning %s not yet supported", holder.getClass().getSimpleName()));
	}

	/**
	 *
	 * @param holder
	 * @return
	 *
	 */
	public SeffBehaviorContextHolder cloneRootBehaviorContextHolder(final RootBehaviorContextHolder holder) {

		final ResourceDemandingBehaviour behaviour = getMatchingPCMElement(
				holder.getCurrentProcessedBehavior().getBehavior());
		final AbstractAction current = getMatchingPCMElement(holder.getCurrentProcessedBehavior().getCurrentAction());

		final SeffBehaviorContextHolder clonedHolder = new RootBehaviorContextHolder(behaviour);

		while (clonedHolder.getCurrentProcessedBehavior().getCurrentAction() != current) {
			clonedHolder.getNextAction();
		}

		return clonedHolder;
	}

	/**
	 *
	 * @param branchedholder
	 * @param clonedParent
	 * @return
	 */
	public SeffBehaviorContextHolder cloneBranchBehaviorContextHolder(final BranchBehaviorContextHolder branchedholder,
			final SeffBehaviorWrapper clonedParent) {

		SeffBehaviorContextHolder clonedHolder = null;
		final ResourceDemandingBehaviour behaviour = getMatchingPCMElement(
				branchedholder.getCurrentProcessedBehavior().getBehavior());
		final AbstractAction current = getMatchingPCMElement(
				branchedholder.getCurrentProcessedBehavior().getCurrentAction());

		// PCM element -> no cloning
		final AbstractAction successor = branchedholder.getSuccessor().isPresent()
				? getMatchingPCMElement(branchedholder.getSuccessor().get())
						: null;

		clonedHolder = new BranchBehaviorContextHolder(behaviour, successor, clonedParent);

		while (clonedHolder.getCurrentProcessedBehavior().getCurrentAction() != current) {
			clonedHolder.getNextAction();
		}

		return clonedHolder;
	}

	/**
	 *
	 * @param calledFrom
	 * @return
	 * @throws CloningFailedException
	 */
	public SEFFInterpretationContext cloneCalledFrom(final SEFFInterpretationContext calledFrom) {
		return cloneContext(calledFrom);
	}

	/**
	 *
	 * @param requestProcessingContext
	 * @return
	 * @throws CloningFailedException
	 */
	public RequestProcessingContext cloneRequestProcessingContext(
			final RequestProcessingContext requestProcessingContext) {

		final ProvidedRole opProvidedRole = getMatchingPCMElement(requestProcessingContext.getProvidedRole());
		final AssemblyContext assmemblyContext = getMatchingPCMElement(requestProcessingContext.getAssemblyContext());

		// Because right now, they might in fact be legitly null :/

		final User clonedUser = cloneUser(requestProcessingContext.getUser());

		final UserInterpretationContext clonedUserInterpretationContext = cloneUserInterpretationContext(
				requestProcessingContext.getUserInterpretationContext(), clonedUser);
		final UserRequest clonedUserRequest = cloneUserRequest(requestProcessingContext.getUserRequest(), clonedUser);

		final RequestProcessingContext clonedRequestProcessingContext = RequestProcessingContext.builder()
				.withUser(clonedUser).withUserRequest(clonedUserRequest)
				.withUserInterpretationContext(clonedUserInterpretationContext)
				.withProvidedRole(opProvidedRole).withAssemblyContext(assmemblyContext).build();

		return clonedRequestProcessingContext;
	}

	/**
	 *
	 *
	 * @param userRequest the request to be cloned. be ware, it might be null.
	 * @return cloned user request, or null, if the request is null.
	 */
	public UserRequest cloneUserRequest(final UserRequest userRequest) {

		// because sometimes the userRequest is legitly null.
		if (userRequest == null) {
			return null;
		}

		return cloneUserRequest(userRequest, cloneUser(userRequest.getUser()));
	}

	/**
	 *
	 * @param userRequest
	 * @return
	 * @throws CloningFailedException
	 */
	public UserRequest cloneUserRequest(final UserRequest userRequest, final User user) {

		// because sometimes the userRequest is legitly null.
		if (userRequest == null) {
			return null;
		}

		final OperationProvidedRole opProvidedRole = getMatchingPCMElement(userRequest.getOperationProvidedRole());
		final OperationSignature signature = getMatchingPCMElement(userRequest.getOperationSignature());
		final EList<VariableUsage> inputParameterUsages = new BasicEList<>(
				userRequest.getVariableUsages().stream().map(usage -> getMatchingPCMElement(usage)).toList());
		final EList<VariableUsage> outputParameterUsages = new BasicEList<>(
				userRequest.getOutVariableUsages().stream().map(usage -> getMatchingPCMElement(usage)).toList());

		final UserRequest clonedUserRequest = UserRequest.builder().withUser(user)
				.withOperationProvidedRole(opProvidedRole).withOperationSignature(signature)
				.withVariableUsages(inputParameterUsages).withOutVariableUsages(outputParameterUsages).build();
		return clonedUserRequest;
	}

	/**
	 * Just cannot figure out whether i could do this manually like with all the
	 * other (non serializable) objects. serialising and deserialising is horribly
	 * tempting though.
	 *
	 * What the fuck to my former self, but also i guess this saves my ass wrt to
	 * the stack?
	 *
	 * @param user the user to be cloned
	 * @return clone of the given user.
	 */
	public User cloneUser(final User user) {

		if (this.alreadyClonedUsers.containsKey(user.getId())) {
			return this.alreadyClonedUsers.get(user.getId());
		}

		try (ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream();
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {

			objectOutputStream.writeObject(user);
			objectOutputStream.flush();

			try (ByteArrayInputStream fileInputStream = new ByteArrayInputStream(fileOutputStream.toByteArray());
					ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {

				final User clone = (User) objectInputStream.readObject();

				assert (clone.getStack().size() == user.getStack().size())
				: String.format("inconsistent stack size, original is %d, clone is %d", user.getStack().size(),
						clone.getStack().size());

				this.alreadyClonedUsers.put(user.getId(), clone);

				return clone;
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Redirect without user.
	 *
	 * @see
	 * @param userInterpretationContext
	 * @return
	 */
	public UserInterpretationContext cloneUserInterpretationContext(
			final UserInterpretationContext userInterpretationContext) {

		if (userInterpretationContext == null) {
			return null;
		}

		return cloneUserInterpretationContext(userInterpretationContext,
				cloneUser(userInterpretationContext.getUser()));
	}

	/**
	 *
	 * Actual clone operation
	 *
	 * @param userInterpretationContext
	 * @param clonedUser
	 * @return
	 */
	public UserInterpretationContext cloneUserInterpretationContext(
			final UserInterpretationContext userInterpretationContext, final User clonedUser) {

		if (userInterpretationContext == null) {
			return null;
		}

		// only clone it once. relevant because nested SEFF interpretations must
		// reference the same UserInterpretationContext...i guess.
		// nah, i really dont remember why it is like this :x
		// TODO dont do this, this is horrible, because the user get out of sync o_O
		// if (this.contextClone != null) {
		// return this.contextClone;
		// }

		UserInterpretationContext clonedUserInterpretationContext = null;
		final AbstractUserAction firstAction = this.getMatchingPCMElement(userInterpretationContext.getCurrentAction());
		final UsageScenario usageScenario = this.getMatchingPCMElement(userInterpretationContext.getScenario());
		final UsageScenarioBehaviorContext scenarioContext = cloneUsageScenarioBehaviorContext(
				userInterpretationContext.getBehaviorContext());

		if (userInterpretationContext instanceof ClosedWorkloadUserInterpretationContext) {

			final ThinkTime thinkTime = ((ClosedWorkloadUserInterpretationContext) userInterpretationContext)
					.getThinkTime();

			clonedUserInterpretationContext = ClosedWorkloadUserInterpretationContext.builder()
					.withUser(clonedUser).withScenario(usageScenario)
					.withCurrentAction(firstAction).withThinkTime(thinkTime)
					.withUsageScenarioBehaviorContext(scenarioContext).build();
		} else {
			final InterArrivalTime interArrivalTime = ((OpenWorkloadUserInterpretationContext) userInterpretationContext)
					.getInterArrivalTime();

			clonedUserInterpretationContext = OpenWorkloadUserInterpretationContext.builder()
					.withUser(clonedUser).withScenario(usageScenario)
					.withCurrentAction(firstAction).withInterArrivalTime(interArrivalTime)
					.withUsageScenarioBehaviorContext(scenarioContext).build();

		}

		return clonedUserInterpretationContext;
	}

	/**
	 *
	 * @param scenarioContext
	 * @return
	 */
	public UsageScenarioBehaviorContext cloneUsageScenarioBehaviorContext(
			final UsageScenarioBehaviorContext scenarioContext) {

		UsageScenarioBehaviorContext clonedContext;

		final ScenarioBehaviour scenarioBehaviour = getMatchingPCMElement(scenarioContext.getScenarioBehavior());

		final Optional<AbstractUserAction> abstractUserAction = scenarioContext.getNextAction().isPresent()
				? Optional.of(getMatchingPCMElement(scenarioContext.getNextAction().get()))
						: Optional.empty();

		final Optional<UsageScenarioBehaviorContext> parent = scenarioContext.getParent().isPresent()
				? Optional.of(cloneUsageScenarioBehaviorContext(scenarioContext.getParent().get()))
						: Optional.empty();

		if (scenarioContext instanceof RootScenarioContext) {

			clonedContext = RootScenarioContext.builder()
					.withNextAction(abstractUserAction)
					// .withParent() no parent for root
					.withScenarioBehavior(scenarioBehaviour)
					.build();

			// TODO : Dont care about "current action, as it is updated by the enclosing
			// Interpretaiton context(?)"
		} else if (scenarioContext instanceof BranchScenarioContext) {
			clonedContext = BranchScenarioContext.builder()
					.withNextAction(abstractUserAction)
					.withParent(parent)
					.withScenarioBehavior(scenarioBehaviour)
					.build();

		} else if (scenarioContext instanceof final LoopScenarioBehaviorContext loopContext) {
			clonedContext = loopContext.update()
					.withNextAction(abstractUserAction)
					.withParent(parent)
					.withScenarioBehavior(scenarioBehaviour)
					.build();

		} else {
			throw new IllegalArgumentException(String.format("Unexpected %s of type %s",
					UsageScenarioBehaviorContext.class.getSimpleName(), scenarioContext.getClass().getSimpleName()));
		}

		return clonedContext;
	}

	/**
	 * TODO PLAIN
	 *
	 * @param generalEntryRequest
	 * @return
	 * @throws CloningFailedException
	 */
	public GeneralEntryRequest cloneGeneralEntryRequest(final GeneralEntryRequest generalEntryRequest) {

		final SEFFInterpretationContext clonedContext = cloneContext(generalEntryRequest.getRequestFrom());

		final EList<VariableUsage> inputParameterUsages = new BasicEList<>(
				generalEntryRequest.getInputVariableUsages().stream().map(usage -> getMatchingPCMElement(usage))
				.toList());
		final EList<VariableUsage> outputParameterUsages = new BasicEList<>(
				generalEntryRequest.getOutputVariableUsages().stream().map(usage -> getMatchingPCMElement(usage))
				.toList());
		final RequiredRole requiredRole = getMatchingPCMElement(generalEntryRequest.getRequiredRole());
		final Signature signature = getMatchingPCMElement(generalEntryRequest.getSignature());

		final GeneralEntryRequest clonedGeneralEntryRequest = GeneralEntryRequest.builder()
				.withInputVariableUsages(inputParameterUsages)
				.withOutputVariableUsages(outputParameterUsages)
				.withRequestFrom(clonedContext)
				.withRequiredRole(requiredRole)
				.withSignature(signature)
				.withUser(clonedContext.getRequestProcessingContext().getUser())
				.build();

		return clonedGeneralEntryRequest;
	}

	/**
	 * Cloning Nested {@link GeneralEntryRequest}s.
	 *
	 * Must reference the same caller {@link SEFFInterpretationContext} as the
	 * enclosing context.
	 *
	 * @param generalEntryRequest
	 * @return
	 * @throws CloningFailedException
	 */
	public GeneralEntryRequest cloneGeneralEntryRequest(final GeneralEntryRequest generalEntryRequest,
			final SEFFInterpretationContext SEFFIC_requestFrom) {

		final EList<VariableUsage> inputParameterUsages = new BasicEList<>(
				generalEntryRequest.getInputVariableUsages().stream().map(usage -> getMatchingPCMElement(usage))
				.toList());
		final EList<VariableUsage> outputParameterUsages = new BasicEList<>(
				generalEntryRequest.getOutputVariableUsages().stream().map(usage -> getMatchingPCMElement(usage))
				.toList());
		final RequiredRole requiredRole = getMatchingPCMElement(generalEntryRequest.getRequiredRole());
		final Signature signature = getMatchingPCMElement(generalEntryRequest.getSignature());

		final GeneralEntryRequest clonedGeneralEntryRequest = GeneralEntryRequest.builder()
				.withInputVariableUsages(inputParameterUsages)
				.withOutputVariableUsages(outputParameterUsages)
				.withRequestFrom(SEFFIC_requestFrom)
				.withRequiredRole(requiredRole)
				.withSignature(signature)
				.withUser(SEFFIC_requestFrom.getRequestProcessingContext().getUser()).build();

		return clonedGeneralEntryRequest;
	}

	/**
	 * TODO
	 *
	 * @param <T>
	 *
	 * @param seffBehaviorWrapper
	 * @return
	 */
	// public SeffBehaviorWrapper cloneSeffBehaviorWrapper(final SeffBehaviorWrapper
	// seffBehaviorWrapper) {
	//
	// final ResourceDemandingBehaviour rdBehaviour =
	// seffBehaviorWrapper.getBehavior();
	// final SeffBehaviorContextHolder clonedSeffBehaviorContextHolder =
	// cloneSeffBehaviorContextHolder(
	// seffBehaviorWrapper.getContext(), null);
	//
	// final SeffBehaviorWrapper clonedSeffBehaviorWrapper = new
	// SeffBehaviorWrapper(rdBehaviour,
	// clonedSeffBehaviorContextHolder);
	//
	// return clonedSeffBehaviorWrapper;
	// }

	/**
	 * Finds a model element in {@code this} helper's resource set that is equal to
	 * the given element.
	 *
	 * If {@code element} is {@code null}, it matches to {@code null}. This is
	 * important, because some Reference to PCM elements in the Slingshot entities
	 * are deliberately {@code null}, e.g. if no next action exits.
	 *
	 *
	 * @param <T>     Type of the element to be matched
	 * @param element element to be matched, if it is not null, it must be contained
	 *                in a resource.
	 * @return matching element from {@code this}' resource set, or {@code null}.,
	 *         if {@code element} was {@code null}.
	 */
	public <T extends EObject> T getMatchingPCMElement(final T element) {
		//		assert element == null || (element != null && element.eResource() != null)
		//				: String.format("Element %s is not contained in a resource, but must be.", element.toString());

		if (element == null) {
			return null;
		}

		// should not load unwanted resources, because there is no resource...?
		if (element.eResource() == null) {
			return element;
		}

		final String fragment = EcoreUtil.getURI(element).fragment();

		final Resource opt = set.getResourceSet().getResources().stream()
				.filter(r -> element.eResource().getClass().isInstance(r)).findFirst()
				.orElseThrow(() -> new NoSuchElementException(
						String.format("No matching resource for %s", element.eResource().getClass().getSimpleName())));

		final T matchedElement = (T) opt.getEObject(fragment);

		assert matchedElement != null : String.format("Missing Element for fragment %s", fragment);

		return matchedElement;
	}

}
