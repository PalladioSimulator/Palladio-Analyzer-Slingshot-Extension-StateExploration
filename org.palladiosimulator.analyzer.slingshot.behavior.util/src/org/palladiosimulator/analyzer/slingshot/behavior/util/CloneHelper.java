package org.palladiosimulator.analyzer.slingshot.behavior.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.LinkingJob;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.GeneralEntryRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.resource.CallOverWireRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.resource.ResourceDemandRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.SEFFInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.RootBehaviorContextHolder;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorContextHolder;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.user.RequestProcessingContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.InterArrivalTime;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.ThinkTime;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.User;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.UserRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.ClosedWorkloadUserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.OpenWorkloadUserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.UserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.scenariobehavior.UsageScenarioBehaviorContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.ClosedWorkloadUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.InterArrivalUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.parameter.VariableUsage;
import org.palladiosimulator.pcm.repository.OperationProvidedRole;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;

import de.uka.ipd.sdq.simucomframework.variables.stackframe.SimulatedStackframe;

/**
 * Helper to create deep copies of entities.
 *
 *
 *
 * @author stiesssh
 *
 */
public final class CloneHelper {

	public static final Logger LOGGER = Logger.getLogger(CloneHelper.class);



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
		return new ClosedWorkloadUserInitiated(
				cloneUserInterpretationContext(event.getEntity()), newThinktime);
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
			final double offset = simulationTime - event.time();
			final UsageModelPassedElement<Start> clonedEvent = new UsageModelPassedElement<Start>((Start) modelElement,
					cloneUserInterpretationContext(((UsageModelPassedElement<?>) event).getContext()));

			clonedEvent.setTime(offset);
			return clonedEvent;
		}
		return event;
	}


	/**
	 *
	 * TODO ??
	 *
	 * @param event
	 * @return
	 */
	public DESEvent clone(final UsageModelPassedElement<?> event) {
		final Object modelElement = event.getModelElement();
		return  new UsageModelPassedElement<Start>((Start) modelElement,
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
		return new ClosedWorkloadUserInitiated(
				cloneUserInterpretationContext(event.getEntity()), newThinktime);
	}


	// getter for various entity clones

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
	 * @param job
	 * @return
	 * @throws CloningFailedException
	 */
	public Job clone(final ActiveJob job) {
		final ResourceDemandRequest clonedRequest = clone(job.getRequest());

		final Job clonedJob = ActiveJob.builder().withDemand(job.getDemand()).withId(job.getId())
				.withProcessingResourceType(job.getProcessingResourceType()).withRequest(clonedRequest)
				.withAllocationContext(job.getAllocationContext()).build();

		return clonedJob;
	}

	/**
	 *
	 * @param job
	 * @return
	 * @throws CloningFailedException
	 */
	public Job clone(final LinkingJob job) {
		final CallOverWireRequest clonedRequest = clone(job.getRequest());

		final Job clonedJob = new LinkingJob(job.getId(), job.getDemand(), job.getLinkingResource(), clonedRequest);

		return clonedJob;
	}

	/**
	 *
	 * @param request
	 * @return
	 */
	public CallOverWireRequest clone(final CallOverWireRequest request) {

		final GeneralEntryRequest clonedEntryRequest = cloneGeneralEntryRequest(request.getEntryRequest());
		final User clonedUser = cloneUser(request.getUser());
		final SimulatedStackframe<Object> variablesToConsider = clonedUser.getStack().currentStackFrame();

		final CallOverWireRequest.Builder clonedRequestBuilder = CallOverWireRequest.builder().from(request.getFrom()).to(request.getTo()).signature(request.getSignature())
				.user(clonedUser).entryRequest(clonedEntryRequest).variablesToConsider(variablesToConsider);

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

		final ResourceDemandRequest clonedRequest = ResourceDemandRequest.builder()
				.withResourceType(resourceDemandRequest.getResourceType()).withSeffInterpretationContext(context)
				.withAssemblyContext(resourceDemandRequest.getAssemblyContext())
				.withParametricResourceDemand(resourceDemandRequest.getParametricResourceDemand()).build();

		return clonedRequest;
	}

	/**
	 *
	 * @param context
	 * @return
	 * @throws CloningFailedException
	 */
	public SEFFInterpretationContext cloneContext(final SEFFInterpretationContext context) {

		final SeffBehaviorContextHolder seffBehaviorContextHolder = cloneBehaviorContext(context.getBehaviorContext());
		final RequestProcessingContext requestProcessingContext = cloneRequestProcessingContext(
				context.getRequestProcessingContext());
		final AssemblyContext assemblyContext = context.getAssemblyContext();

		SEFFInterpretationContext calledFrom = null;

		if (context.getCaller().isPresent()) {
			calledFrom = cloneCalledFrom(context.getCaller().get());
		}

		final SEFFInterpretationContext clonedContext = SEFFInterpretationContext.builder()
				.withAssemblyContext(assemblyContext).withCaller(calledFrom)
				.withBehaviorContext(seffBehaviorContextHolder).withRequestProcessingContext(requestProcessingContext)
				.build();

		return clonedContext;
	}

	/**
	 *
	 * @param holder
	 * @return
	 * @throws CloningFailedException
	 */
	public SeffBehaviorContextHolder cloneBehaviorContext(final SeffBehaviorContextHolder holder) {

		SeffBehaviorContextHolder clonedHolder = null;
		final ResourceDemandingBehaviour behaviour = holder.getCurrentProcessedBehavior().getBehavior();
		final AbstractAction current = holder.getCurrentProcessedBehavior().getCurrentAction();

		if (holder instanceof RootBehaviorContextHolder) {

			clonedHolder = new RootBehaviorContextHolder(behaviour);

			while (clonedHolder.getCurrentProcessedBehavior().getCurrentAction() != current) {
				clonedHolder.getNextAction();
			}
		} else {
			throw new IllegalArgumentException(
					String.format("clonging %s not yet implemented", holder.getClass().getSimpleName()));
		}
		// TODO else reset the parents

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

		final ProvidedRole opProvidedRole = requestProcessingContext.getProvidedRole();
		final AssemblyContext assmemblyContext = requestProcessingContext.getAssemblyContext();

		final RequestProcessingContext clonedRequestProcessingContext = RequestProcessingContext.builder()
				.withUser(cloneUser(requestProcessingContext.getUser()))
				.withUserRequest(cloneUserRequest(requestProcessingContext.getUserRequest()))
				.withUserInterpretationContext(
						cloneUserInterpretationContext(requestProcessingContext.getUserInterpretationContext()))
				.withProvidedRole(opProvidedRole).withAssemblyContext(assmemblyContext).build();

		return clonedRequestProcessingContext;
	}

	/**
	 *
	 * @param userRequest
	 * @return
	 * @throws CloningFailedException
	 */
	public UserRequest cloneUserRequest(final UserRequest userRequest) {

		// because sometimes the userRequest is legitly null.
		if (userRequest == null) {
			return null;
		}

		final OperationProvidedRole opProvidedRole = userRequest.getOperationProvidedRole();
		final OperationSignature signature = userRequest.getOperationSignature();
		final EList<VariableUsage> inputParameterUsages = userRequest.getVariableUsages();

		final UserRequest clonedUserRequest = UserRequest.builder().withUser(cloneUser(userRequest.getUser()))
				.withOperationProvidedRole(opProvidedRole).withOperationSignature(signature)
				.withVariableUsages(inputParameterUsages).build();
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
	 * @param user
	 * @return
	 * @throws CloningFailedException
	 */
	public User cloneUser(final User user) {
		try (ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream();
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {

			objectOutputStream.writeObject(user);
			objectOutputStream.flush();

			try (ByteArrayInputStream fileInputStream = new ByteArrayInputStream(fileOutputStream.toByteArray());
					ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {

				final User clone = (User) objectInputStream.readObject();
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
	 *
	 * @param userInterpretationContext
	 * @return
	 * @throws CloningFailedException
	 */
	public UserInterpretationContext cloneUserInterpretationContext(final UserInterpretationContext userInterpretationContext) {

		if (userInterpretationContext == null) {
			return null;
		}

		// only clone it once. relevant because nested SEFF interpretations must
		// reference the same UserInterpretationContext...i guess.
		// nah, i really dont remember why it is like this :x
		// TODO dont do this, this is horrible, because the user get out of sync o_O
		//		if (this.contextClone != null) {
		//			return this.contextClone;
		//		}

		UserInterpretationContext clonedUserInterpretationContext = null;
		final AbstractUserAction firstAction = userInterpretationContext.getCurrentAction();

		if (userInterpretationContext instanceof ClosedWorkloadUserInterpretationContext) {

			final UsageScenario usageScenario = userInterpretationContext.getScenario();
			final ThinkTime thinkTime = ((ClosedWorkloadUserInterpretationContext) userInterpretationContext).getThinkTime();

			final UsageScenarioBehaviorContext scenarioContext = cloneUsageScenarioBehaviorContext(
					userInterpretationContext.getBehaviorContext());

			clonedUserInterpretationContext = ClosedWorkloadUserInterpretationContext.builder()
					.withUser(cloneUser(userInterpretationContext.getUser())).withScenario(usageScenario)
					.withCurrentAction(firstAction).withThinkTime(thinkTime)
					.withUsageScenarioBehaviorContext(scenarioContext).build();
		} else {
			final UsageScenario usageScenario = userInterpretationContext.getScenario();
			final InterArrivalTime interArrivalTime = ((OpenWorkloadUserInterpretationContext) userInterpretationContext)
					.getInterArrivalTime();

			final UsageScenarioBehaviorContext scenarioContext = cloneUsageScenarioBehaviorContext(
					userInterpretationContext.getBehaviorContext());

			clonedUserInterpretationContext = OpenWorkloadUserInterpretationContext.builder()
					.withUser(cloneUser(userInterpretationContext.getUser())).withScenario(usageScenario)
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
		// TODO ich will jetzt erstmal durch das SEFF durchkommen...
		// the only reason this has not yet broken my leg, is because i always work with
		// root scenarios of a single action.

		//		RootScenarioContext clonedScenarioContext = RootScenarioContext.builder()
		//				.withScenarioBehavior(scenarioContext.getScenarioBehavior())
		//				.build();

		return scenarioContext;
	}

	/**
	 * TODO
	 *
	 * @param generalEntryRequest
	 * @return
	 * @throws CloningFailedException
	 */
	public GeneralEntryRequest cloneGeneralEntryRequest(final GeneralEntryRequest generalEntryRequest) {

		final GeneralEntryRequest clonedGeneralEntryRequest = GeneralEntryRequest.builder()
				.withInputVariableUsages(generalEntryRequest.getInputVariableUsages())
				.withRequestFrom(cloneContext(generalEntryRequest.getRequestFrom()))
				.withRequiredRole(generalEntryRequest.getRequiredRole())
				.withSignature(generalEntryRequest.getSignature()).withUser(cloneUser(generalEntryRequest.getUser()))
				.build();

		return clonedGeneralEntryRequest;
	}

}
