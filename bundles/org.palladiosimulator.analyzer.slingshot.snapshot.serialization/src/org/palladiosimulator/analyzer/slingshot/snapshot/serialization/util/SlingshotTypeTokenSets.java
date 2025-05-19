package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.util;

import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
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
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.ThinkTime;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.User;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.UserRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.ClosedWorkloadUserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.OpenWorkloadUserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.UserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.scenariobehavior.RootScenarioContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.scenariobehavior.UsageScenarioBehaviorContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.InterArrivalUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.monitor.data.entities.SlingshotMeasuringValue;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementUpdated.MeasurementUpdateInformation;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.factories.EntityTypeAdapterFactory;
import org.palladiosimulator.measurementframework.BasicMeasurement;
import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.measurementframework.TupleMeasurement;
import org.palladiosimulator.metricspec.metricentity.MetricEntity;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.impl.StopActionImpl;
import org.palladiosimulator.pcm.usagemodel.Start;

import com.google.gson.reflect.TypeToken;

public final class SlingshotTypeTokenSets {
	
	
	/**
	 * Set of all classes for which a {@link EntityTypeAdapterFactory} should be used to create an adapter.
	 */
	protected final static Set<TypeToken<?>> typeSetEntities = Set.of(TypeToken.get(UserInterpretationContext.class),
			TypeToken.get(MetricEntity.class),
			TypeToken.get(BasicMeasurement.class),
			TypeToken.get(User.class),
			TypeToken.get(MeasuringValue.class),
			TypeToken.get(SlingshotMeasuringValue.class),
			TypeToken.get(OpenWorkloadUserInterpretationContext.class),
			TypeToken.get(ClosedWorkloadUserInterpretationContext.class),
			TypeToken.get(ResourceDemandRequest.class),
			TypeToken.get(CallOverWireRequest.class),
			TypeToken.get(SeffBehaviorWrapper.class),
			TypeToken.get(SEFFInterpretationContext.class),
			TypeToken.get(RequestProcessingContext.class),
			TypeToken.get(RootBehaviorContextHolder.class),
			TypeToken.get(TupleMeasurement.class),
			TypeToken.get(UsageScenarioBehaviorContext.class),
			TypeToken.get(ActiveJob.class),
			TypeToken.get(BranchBehaviorContextHolder.class),
			TypeToken.get(Job.class),
			TypeToken.get(SeffBehaviorContextHolder.class),
			TypeToken.get(UserRequest.class),
			TypeToken.get(RootScenarioContext.class),
			TypeToken.get(GeneralEntryRequest.class),
			TypeToken.get(ThinkTime.class),
			TypeToken.get(MeasurementUpdateInformation.class));

/**
 * Set of all classes, that appear as value types inside an option. 
 */
protected final static Set<TypeToken<?>> typeSetOptionals = Set.of(
		TypeToken.get(StopActionImpl.class),
		TypeToken.get(CallOverWireRequest.class),
		TypeToken.get(SeffBehaviorWrapper.class),
		TypeToken.get(SEFFInterpretationContext.class));

/**
 * Set of all classes, that appear as value types inside an option. 
 */
protected final static Set<TypeToken<?>> typeSetDESEvents = Set.of(TypeToken.get(JobInitiated.class),
		TypeToken.getParameterized(UsageModelPassedElement.class, Start.class),
		TypeToken.getParameterized(SEFFModelPassedElement.class, StartAction.class),
		TypeToken.get(InterArrivalUserInitiated.class));

	

//private final static Map<String, Class<? extends DESEvent>> eventTypeMap = Map.of(
//	CallOverWireAborted.class.getCanonicalName(), CallOverWireAborted.class,
//	CallOverWireRequested.class.getCanonicalName(), CallOverWireRequested.class,
//	CallOverWireSucceeded.class.getCanonicalName(), CallOverWireSucceeded.class,
//	SEFFModelPassedElement.class.getCanonicalName(), SEFFModelPassedElement.class,
//	UsageModelPassedElement.class.getCanonicalName(), UsageModelPassedElement.class,
//	ActiveResourceStateUpdated.class.getCanonicalName(), ActiveResourceStateUpdated.class,
//
//
////,
//	JobAborted.class.getCanonicalName(), JobAborted.class,
//	JobFinished.class.getCanonicalName(), JobFinished.class,
//	JobInitiated.class.getCanonicalName(), JobInitiated.class,
//	JobProgressed.class.getCanonicalName(), JobProgressed.class,
//	ProcessorSharingJobProgressed.class.getCanonicalName(), ProcessorSharingJobProgressed.class,
//	ResourceDemandCalculated.class.getCanonicalName(), ResourceDemandCalculated.class,
//	ActiveResourceFinished.class.getCanonicalName(), ActiveResourceFinished.class,
//	PassiveResourceAcquired.class.getCanonicalName(), PassiveResourceAcquired.class,
//	PassiveResourceReleased.class.getCanonicalName(), PassiveResourceReleased.class);
////	PassiveResourceStateUpdated.class.getCanonicalName(), PassiveResourceStateUpdated.class,
////	ResourceDemandRequestAborted.class.getCanonicalName(), ResourceDemandRequestAborted.class,
////	ResourceDemandRequested.class.getCanonicalName(), ResourceDemandRequested.class,
////	ResourceRequestFailed.class.getCanonicalName(), ResourceRequestFailed.class,
////	SEFFChildInterpretationStarted.class.getCanonicalName(), SEFFChildInterpretationStarted.class,
////	SEFFInterpretationFinished.class.getCanonicalName(), SEFFInterpretationFinished.class,
////	SEFFInterpretationProgressed.class.getCanonicalName(), SEFFInterpretationProgressed.class,
////	ClosedWorkloadUserInitiated.class.getCanonicalName(), ClosedWorkloadUserInitiated.class,
////	InnerScenarioBehaviorInitiated.class.getCanonicalName(), InnerScenarioBehaviorInitiated.class,
////	InterArrivalUserInitiated.class.getCanonicalName(), InterArrivalUserInitiated.class,
////	UsageScenarioFinished.class.getCanonicalName(), UsageScenarioFinished.class,
////	UsageScenarioStarted.class.getCanonicalName(), UsageScenarioStarted.class,
////	UserAborted.class.getCanonicalName(), UserAborted.class,
////	UserFinished.class.getCanonicalName(), UserFinished.class,
////	UserSlept.class.getCanonicalName(), UserSlept.class,
////	UserStarted.class.getCanonicalName(), UserStarted.class,
////	UserWokeUp.class.getCanonicalName(), UserWokeUp.class,
////	JobScheduled.class.getCanonicalName(), JobScheduled.class,
////	MeasurementMade.class.getCanonicalName(), MeasurementMade.class,
////	MeasurementUpdated.class.getCanonicalName(), MeasurementUpdated.class,
////	ProbeTaken.class.getCanonicalName(), ProbeTaken.class,
////	SEFFExternalActionCalled.class.getCanonicalName(), SEFFExternalActionCalled.class,
////	SEFFInfrastructureCalled.class.getCanonicalName(), SEFFInfrastructureCalled.class,
////	UserEntryRequested.class.getCanonicalName(), UserEntryRequested.class,
////	UserInterpretationProgressed.class.getCanonicalName(), UserInterpretationProgressed.class,
////	UserRequestFinished.class.getCanonicalName(), UserRequestFinished.class,
////	IntervalPassed.class.getCanonicalName(), IntervalPassed.class,
////	SimulationTimeReached.class.getCanonicalName(), SimulationTimeReached.class)
//
////	ModelAdjusted.class.getCanonicalName(), ModelAdjusted.class);
////	ModelAdjustmentRequested.class.getCanonicalName(), ModelAdjustmentRequested.class);
////	PreSimulationConfigurationStarted.class.getCanonicalName(),
////			PreSimulationConfigurationStarted.class);
////	ProcessingTypeRevealed.class.getCanonicalName(), ProcessingTypeRevealed.class);
////	SimulationFinished.class.getCanonicalName(), SimulationFinished.class);
////	SimulationStarted.class.getCanonicalName(), SimulationStarted.class);
////	SnapshotInitiated.class.getCanonicalName(), SnapshotInitiated.class);
////	SnapshotTaken.class.getCanonicalName(), SnapshotTaken.class);
////	SPDAdjustorStateInitialized.class.getCanonicalName(), SPDAdjustorStateInitialized.class);
////	TakeCostMeasurement.class.getCanonicalName(), TakeCostMeasurement.class);
////	MonitorModelVisited.class.getCanonicalName(), MonitorModelVisited.class);
////	MeasurementSpecificationVisited.class.getCanonicalName(), MeasurementSpecificationVisited.class);
////	ProcessingTypeVisited.class.getCanonicalName(), ProcessingTypeVisited.class);
////	RepositoryInterpretationInitiated.class.getCanonicalName(),
////	RepositoryInterpretationInitiated.class);
////		CalculatorRegistered.class.getCanonicalName(), CalculatorRegistered.class);
}
