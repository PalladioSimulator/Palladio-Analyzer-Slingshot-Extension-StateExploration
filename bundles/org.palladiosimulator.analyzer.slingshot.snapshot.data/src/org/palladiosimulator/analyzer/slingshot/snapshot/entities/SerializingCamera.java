package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.AbstractJobEvent;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.ActiveResourceStateUpdated;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobAborted;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobProgressed;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobScheduled;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.PassiveResourceStateUpdated;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.ProcessorSharingJobProgressed;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.ResourceDemandCalculated;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SimulationTimeReached;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.GeneralEntryRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.resource.CallOverWireRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.resource.ResourceDemandRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.SEFFInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorContextHolder;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.seff.behaviorcontext.SeffBehaviorWrapper;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.entities.user.RequestProcessingContext;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.ActiveResourceFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.CallOverWireAborted;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.CallOverWireRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.CallOverWireSucceeded;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.PassiveResourceAcquired;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.PassiveResourceReleased;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.RepositoryInterpretationInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.ResourceDemandRequestAborted;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.ResourceDemandRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.ResourceRequestFailed;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFChildInterpretationStarted;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFExternalActionCalled;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFInfrastructureCalled;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFInterpretationFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFInterpretationProgressed;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.usageevolution.events.IntervalPassed;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.ThinkTime;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.User;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.UserRequest;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.interpretationcontext.UserInterpretationContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.scenariobehavior.UsageScenarioBehaviorContext;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.ClosedWorkloadUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.InnerScenarioBehaviorInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.InterArrivalUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageScenarioFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageScenarioStarted;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserAborted;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserEntryRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserInterpretationProgressed;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserRequestFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserSlept;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserStarted;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserWokeUp;
import org.palladiosimulator.analyzer.slingshot.behavior.util.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.common.utils.events.ModelPassedEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.core.events.PreSimulationConfigurationStarted;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.cost.events.TakeCostMeasurement;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.CalculatorRegistered;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementMade;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementUpdated;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementUpdated.MeasurementUpdateInformation;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.ProbeTaken;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.ProcessingTypeRevealed;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.modelvisited.MeasurementSpecificationVisited;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.modelvisited.MonitorModelVisited;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.modelvisited.ProcessingTypeVisited;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Camera;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.EventRecord;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotTaken;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.metricspec.metricentity.MetricEntity;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.usagemodel.Start;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

import de.uka.ipd.sdq.scheduler.resources.active.AbstractActiveResource;
import spielwiese.version2.EventAndType;
import spielwiese.version2.adapters.ClassTypeAdapter;
import spielwiese.version2.adapters.EObjectTypeAdapter;
import spielwiese.version2.adapters.TypeTokenTypeAdapter;
import spielwiese.version2.factories.ElistTypeAdapterFactory;
import spielwiese.version2.factories.NonParameterizedCustomizedTypeAdapterFactory2;
import spielwiese.version2.factories.OptionalTypeAdapterFactory;
import spielwiese.version2.factories.SEFFBehaviourContextHolderTypeAdapterFactory;
import spielwiese.version2.factories.SEFFBehaviourWrapperTypeAdapterFactory;

/**
 * 
 * This class creates a {@link Snapshot} based on a the record of a
 * {@link EventRecord} and the future events from the {@link SimulationEngine}.
 * 
 * All events in the snapshot are copies of the originals. Also, delays are
 * already adjusted and offsets for resending those events are encoded into
 * {@link DESEvent#time}.
 * 
 * 
 * @author Sophie Stieß
 *
 */
public final class SerializingCamera implements Camera {
	private static final Logger LOGGER = Logger.getLogger(SerializingCamera.class);

	/** Beware: keep in sync with original */
	private static final String FAKE = "fakeID";

	/** Access to past events, that must go into the snapshot. */
	private final LessInvasiveInMemoryRecord record;

	/** Access to future events, that must go into the snapshot. */
	private final SimulationEngine engine;

	private final LambdaVisitor<DESEvent, DESEvent> adjustOffset;
	
	/** Required argument for creating clone helpers */
	private final PCMResourceSetPartition set;

	private final List<DESEvent> additionalEvents = new ArrayList<>();

	private final Path location;
	private final String fileName = "events.json";

	public SerializingCamera(final LessInvasiveInMemoryRecord record, final SimulationEngine engine,
			final PCMResourceSetPartition set) {
		this.record = record;
		this.engine = engine;

		this.set = set;

		this.adjustOffset = new LambdaVisitor<DESEvent, DESEvent>()
				.on(UsageModelPassedElement.class).then(this::clone)
				.on(SEFFModelPassedElement.class).then(this::clone)
				.on(ClosedWorkloadUserInitiated.class).then(this::clone)
				.on(InterArrivalUserInitiated.class).then(this::clone)
				.on(DESEvent.class).then(e -> e);

		final String folder = set.getAllocation().eResource().getURI().trimSegments(1).toFileString();

		final Path path = FileSystems.getDefault().getPath(folder, fileName);

		location = path;
	}

	@Override
	public Snapshot takeSnapshot() {
		this.getScheduledReconfigurations().forEach(this::addEvent);
		final Snapshot snapshot = new JsonSnapshot(snapEvents());
		return snapshot;
	}

	public Set<DESEvent> read(final File file) {
		return 	(new Serializer(set.getAllocation().eResource().getResourceSet()))
				.deserialize(file);
	}

	/*
	 * Creation of offsetted Events. some of them could work without the creation of
	 * new events, but not all.
	 */
	private DESEvent clone(final UsageModelPassedElement<?> event) {
		final double simulationTime = engine.getSimulationInformation().currentSimulationTime();
		if (event.getModelElement() instanceof Start && event.time() <= simulationTime) {
			setOffset(event.time(), simulationTime, event);

		}
		return event;
	}

	private DESEvent clone(final SEFFModelPassedElement<?> event) {
		final double simulationTime =  engine.getSimulationInformation().currentSimulationTime();

		if (event.getModelElement() instanceof StartAction && event.time() <= simulationTime) {	
			setOffset(event.time(), simulationTime, event);
		}
		return event;
	}

	private DESEvent clone(final ClosedWorkloadUserInitiated event) {
		final double simulationTime = engine.getSimulationInformation().currentSimulationTime();
		
		final double remainingthinktime = event.time() - simulationTime;

		final CoreFactory coreFactory = CoreFactory.eINSTANCE;
		final PCMRandomVariable var = coreFactory.createPCMRandomVariable();
		var.setSpecification(String.valueOf(remainingthinktime));

		final ThinkTime newThinktime = new ThinkTime(var);
		return new ClosedWorkloadUserInitiated(event.getEntity(), newThinktime);
	}

	private DESEvent clone(final InterArrivalUserInitiated event) {
		final double simulationTime = engine.getSimulationInformation().currentSimulationTime();
		return new InterArrivalUserInitiated(event.getEntity(), event.time() - simulationTime);
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
	 * @param event    the event to be modified
	 */
	private void setOffset(final double eventTime, final double simulationTime, final ModelPassedEvent<?> event) {
		if (eventTime < 0) {
			final double offset = -(eventTime - simulationTime);
			event.setTime(offset);
		} else {
			final double offset = simulationTime - eventTime;
			event.setTime(offset);
		}
	}

	/**
	 * Collect and clone all state relevant events from the past and the future and
	 * adjust offsetts, if necessary.
	 *
	 * @return Set of events for recreating the state.
	 */
	private String snapEvents() {
		// get FEL
		final Set<DESEvent> relevantEvents = engine.getScheduledEvents();

		// get events to recreate state of queues
		final Set<JobRecord> fcfsRecords = record.getFCFSJobRecords();
		final Set<JobRecord> procsharingRecords = record.getProcSharingJobRecords();

		final Set<AbstractJobEvent> progressedFcfs = relevantEvents.stream()
				.filter(e -> (e instanceof JobProgressed) || (e instanceof JobFinished)).map(e -> (AbstractJobEvent) e)
				.collect(Collectors.toSet());

		final Set<JobInitiated> initJobs = new HashSet<>();
		initJobs.addAll(this.handlePFCFSJobs(fcfsRecords, progressedFcfs));
		initJobs.addAll(this.handleProcSharingJobs(procsharingRecords));

		relevantEvents.addAll(initJobs);
		relevantEvents.addAll(record.getRecordedCalculators());

		this.removeFakeThings(relevantEvents);

		final Set<DESEvent> offsettedEvents = relevantEvents.stream().map(adjustOffset).collect(Collectors.toSet());

		final String clonedEvents = (new Serializer(set.getAllocation().eResource().getResourceSet()))
				.serialize(offsettedEvents); // Serialise offsettedEvents

		return clonedEvents;
	}

	
	private void removeFakeThings(final Set<DESEvent> events) {
		events.removeIf(e -> this.isFake(e));
	}
	
	private boolean isFake(final DESEvent event) {
		if (event instanceof final AbstractJobEvent jobEvent) {
			return jobEvent.getEntity().getId().equals(FAKE);
		} else {
			return false;
		}
	}
	
	/**
	 *
	 * Get {@link ModelAdjustmentRequested} events, that happened at the point in
	 * time the snapshot was taken, but did not trigger it.
	 *
	 * As there is no guarantee on the order of events, that happen at the same
	 * point in time, the {@link ModelAdjustmentRequested} events are either
	 * directly scheduled, or already wrapped into {@link SnapshotInitiated} or
	 * {@link SnapshotTaken} events.
	 *
	 * @return upcoming {@link ModelAdjustmentRequested} events.
	 */
	private List<ModelAdjustmentRequested> getScheduledReconfigurations() {
		final List<ModelAdjustmentRequested> events = new ArrayList<>();

		/* Scheduled ModelAdjustmentRequested */
		engine.getScheduledEvents().stream().filter(ModelAdjustmentRequested.class::isInstance)
				.map(ModelAdjustmentRequested.class::cast).forEach(events::add);

		/* ModelAdjustmentRequested already processed into SnapshotInitiated events */
		engine.getScheduledEvents().stream().filter(SnapshotInitiated.class::isInstance)
				.map(SnapshotInitiated.class::cast).filter(e -> e.getTriggeringEvent().isPresent())
				.forEach(e -> events.add(e.getTriggeringEvent().get()));

		/* ModelAdjustmentRequested already processed into SnapshotTaken events */
		engine.getScheduledEvents().stream().filter(SnapshotTaken.class::isInstance).map(SnapshotTaken.class::cast)
				.filter(e -> e.getTriggeringEvent().isPresent()).forEach(e -> events.add(e.getTriggeringEvent().get()));

		return events;
	}

	/**
	 * Denormalizes the demand of the open jobs and creates {@link JobInitiated}
	 * events to reinsert them to their respective Processor Sharing Resource.
	 *
	 * C.f. {@link SerializingCamera#handlePFCFSJobs(Set, Set)} for details on the
	 * demand denormalized.
	 * 
	 * @param jobrecords
	 * @return
	 */
	private Set<JobInitiated> handleProcSharingJobs(final Set<JobRecord> jobrecords) {
		final Set<JobInitiated> rval = new HashSet<>();

		for (final JobRecord jobRecord : jobrecords) {
			// do the Proc Sharing Math
			final double ratio = jobRecord.getNormalizedDemand() == 0 ? 0
					: jobRecord.getCurrentDemand() / jobRecord.getNormalizedDemand();
			final double reducedRequested = jobRecord.getRequestedDemand() * ratio;
			jobRecord.getJob().updateDemand(reducedRequested);
			rval.add(new JobInitiated(jobRecord.getJob()));

		}
		return rval;
	}

	/**
	 * Denormalizes the demand of the open jobs and creates {@link JobInitiated}
	 * events to reinsert them to their respective FCFS Resource.
	 *
	 * The demand must be denormalized, because upon receiving a
	 * {@link JobInitiated} event, the {@link AbstractActiveResource} normalizes a
	 * job's demand with the resource's processing rate. Thus without
	 * denormalisation, the demand would be wrong.
	 *
	 * This is required for ActiveJobs, and for LinkingJobs. In case of LinkingJobs,
	 * the throughput is used as processing rate.
	 *
	 * @param jobrecords     jobs waiting at an FCFS resource at the time of the
	 *                       snapshot
	 * @param fcfsProgressed events scheduled for simulation at the time of the
	 *                       snapshot
	 * @return events to reinsert all open jobs to their respective FCFS Resource
	 */
	private Set<JobInitiated> handlePFCFSJobs(final Set<JobRecord> jobrecords,
			final Set<AbstractJobEvent> fcfsProgressed) {
		final Set<JobInitiated> rval = new HashSet<>();

		final Map<Job, AbstractJobEvent> progressedJobs = new HashMap<>();
		fcfsProgressed.stream().forEach(event -> progressedJobs.put(event.getEntity(), event));

		for (final JobRecord record : jobrecords) {
			if (record.getNormalizedDemand() == 0) { // For Linking Jobs.
				if (record.getJob().getDemand() != 0) {
					throw new IllegalStateException(
							String.format("Job %s of Type %s: Normalized demand is 0, but acutal demand is not.",
									record.getJob().toString(), record.getJob().getClass().getSimpleName()));
				}
			} else if (progressedJobs.keySet().contains(record.getJob())) {
				final AbstractJobEvent event = progressedJobs.get(record.getJob());
				// time equals remaining demand because of normalization.
				final double remainingDemand = event.time() - engine.getSimulationInformation().currentSimulationTime();
				final double factor = record.getRequestedDemand() / record.getNormalizedDemand();
				final double denormalizedRemainingDemand = remainingDemand * factor;
				record.getJob().updateDemand(denormalizedRemainingDemand);
			} else {
				record.getJob().updateDemand(record.getRequestedDemand());
			}
			rval.add(new JobInitiated(record.getJob()));
		}
		return rval;
	}

	public class Serializer {

		private final ResourceSet set;
		private final Map<String, Object> objs = new HashMap<>();
		private final Map<String, TypeAdapter<?>> thingTypes = new HashMap<>();

		private final Map<String, Class<? extends DESEvent>> eventTypes = createTypeMap();

		private final Gson gson;
		private final Gson gsonwithAdapter;


		OptionalTypeAdapterFactory fo;
		
		public Serializer(final ResourceSet set) {
			this.set = set;

			// Create Gsons
			final GsonBuilder adaptereBuilder = new GsonBuilder();

			// register direct adapters.
			adaptereBuilder.registerTypeHierarchyAdapter(EObject.class, new EObjectTypeAdapter(set));
			adaptereBuilder.registerTypeHierarchyAdapter(Class.class, new ClassTypeAdapter());
			adaptereBuilder.registerTypeHierarchyAdapter(com.google.common.reflect.TypeToken.class,
					new TypeTokenTypeAdapter());

			// register special factory
			adaptereBuilder.registerTypeAdapterFactory(new SEFFBehaviourContextHolderTypeAdapterFactory(thingTypes));
			adaptereBuilder.registerTypeAdapterFactory(new SEFFBehaviourWrapperTypeAdapterFactory(thingTypes));

			// register factories
			adaptereBuilder.registerTypeAdapterFactory(
					new NonParameterizedCustomizedTypeAdapterFactory2(applicableClasses(), objs, thingTypes));

			fo = new OptionalTypeAdapterFactory();
			adaptereBuilder.registerTypeAdapterFactory(fo);
			adaptereBuilder.registerTypeAdapterFactory(new ElistTypeAdapterFactory());

			gsonwithAdapter = adaptereBuilder.create();

			final GsonBuilder builder = new GsonBuilder();

			builder.registerTypeHierarchyAdapter(EventAndType.class, new JsonDeserializer<DESEvent>() {
				@Override
				public DESEvent deserialize(final JsonElement json, final Type typeOfT,
						final JsonDeserializationContext context) throws JsonParseException {
					if (json.isJsonObject()) {
						final var type = json.getAsJsonObject().get("type");
						final var event = json.getAsJsonObject().get("event");
						if (type != null) {
							final var eventString = type.getAsString();
							if (eventTypes.containsKey(eventString)) {
								return gsonwithAdapter.fromJson(event, eventTypes.get(eventString));
							} else {
								throw new RuntimeException("Unknown message type: " + type);
							}
						}
					}
					throw new RuntimeException("Failed to parse message: " + json);
				}

			});

			builder.registerTypeHierarchyAdapter(EventAndType.class, new JsonSerializer<EventAndType>() {

				@Override
				public JsonElement serialize(final EventAndType src, final Type typeOfSrc,
						final JsonSerializationContext context) {
					final JsonObject obj = new JsonObject();
					obj.addProperty("type", src.getType());
					final JsonElement e = gsonwithAdapter.toJsonTree(src.getEvent());
					obj.add("event", e);

					return obj;
				}

			});

			gson = builder.create();
		}

		/**
		 * 
		 * @param events
		 * @return
		 */
		public String serialize(final Set<DESEvent> events) {

			final Set<EventAndType> eventsWithTypes = createEventAndTypes(events);

			final String eventJsonString = gson.toJson(eventsWithTypes);

			System.out.println(eventJsonString);

			System.out.println("save json to " + location.toFile().toString());

			try (final BufferedWriter writer = new BufferedWriter(new FileWriter(location.toFile()))) {
				writer.write(eventJsonString);
				writer.flush();
			} catch (final IOException e) {
				e.printStackTrace();
			}

			return eventJsonString;
		}

		/**
		 * 
		 * @param events
		 * @return
		 */
		public Set<DESEvent> deserialize(final File file) {
			System.out.println("read json from " + file.toString());

			try (final FileReader reader = new FileReader(file)) {

				final String readString = Files.readString(file.toPath());
				
				final Type set2Type = new TypeToken<Set<EventAndType>>() {
				}.getType();

				final Set<DESEvent> events2 = gson.fromJson(readString, set2Type);

				return events2;
			} catch (final IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		/**
		 * Type the events.
		 * 
		 * @param events
		 * @return
		 */
		private Set<EventAndType> createEventAndTypes(final Set<DESEvent> events) {
			final Set<EventAndType> eventsWithTypes = new HashSet<>();
			final Set<Class<?>> skip = Set.of(SnapshotInitiated.class, SnapshotTaken.class, SnapshotFinished.class,
					ProbeTaken.class, SimulationFinished.class, MeasurementMade.class, MeasurementUpdated.class, TakeCostMeasurement.class,
					IntervalPassed.class, ProcessorSharingJobProgressed.class);

			final Set<Class<?>> error = Set.of(PreSimulationConfigurationStarted.class, SimulationStarted.class,
					MonitorModelVisited.class, ProcessingTypeRevealed.class, CalculatorRegistered.class,
					RepositoryInterpretationInitiated.class, MeasurementSpecificationVisited.class,
					ProcessingTypeVisited.class, ModelAdjusted.class, ModelAdjustmentRequested.class);

			for (final DESEvent event : events) {
				if (!skip.contains(event.getClass())) {
					eventsWithTypes.add(new EventAndType(event, event.getClass().getCanonicalName()));
				}
			}
			return eventsWithTypes;
		}

		/**
		 * Classes listed here get (de)serialized by {@link NonParameterizedCustomizedTypeAdapterFactory2}
		 * @return
		 */
		private Set<Class<?>> applicableClasses() {
			final Set<Class<?>> set = new HashSet<>();

			set.add(UsageScenarioBehaviorContext.class);
			set.add(UserInterpretationContext.class);
			set.add(RequestProcessingContext.class);
			set.add(UserRequest.class);
			set.add(User.class); // ????

			// currently looking at
			set.add(SEFFInterpretationContext.class);
			set.add(SeffBehaviorWrapper.class);
			set.add(SeffBehaviorContextHolder.class); // we have special TypeAdapter for this one.
			set.add(ResourceDemandRequest.class);
			set.add(GeneralEntryRequest.class);
			set.add(CallOverWireRequest.class);
			set.add(Job.class);

			// might work without
			set.add(ThinkTime.class);

			set.add(MetricEntity.class); // maybe optimize with dedicated type adapter
			set.add(MeasuringValue.class);
			
			set.add(MeasurementUpdateInformation.class);

			return set;
		}

		private Map<String, Class<? extends DESEvent>> createTypeMap() {

			final Map<String, Class<? extends DESEvent>> foo = new HashMap<>();

			foo.put(CallOverWireAborted.class.getCanonicalName(), CallOverWireAborted.class);
			foo.put(CallOverWireRequested.class.getCanonicalName(), CallOverWireRequested.class);
			foo.put(CallOverWireSucceeded.class.getCanonicalName(), CallOverWireSucceeded.class);
			foo.put(SEFFModelPassedElement.class.getCanonicalName(), SEFFModelPassedElement.class);
			foo.put(UsageModelPassedElement.class.getCanonicalName(), UsageModelPassedElement.class);
			foo.put(ActiveResourceStateUpdated.class.getCanonicalName(), ActiveResourceStateUpdated.class);
			foo.put(JobAborted.class.getCanonicalName(), JobAborted.class);
			foo.put(JobFinished.class.getCanonicalName(), JobFinished.class);
			foo.put(JobInitiated.class.getCanonicalName(), JobInitiated.class);
			foo.put(JobProgressed.class.getCanonicalName(), JobProgressed.class);
			foo.put(ProcessorSharingJobProgressed.class.getCanonicalName(), ProcessorSharingJobProgressed.class);
			foo.put(ResourceDemandCalculated.class.getCanonicalName(), ResourceDemandCalculated.class);
			foo.put(ActiveResourceFinished.class.getCanonicalName(), ActiveResourceFinished.class);
			foo.put(PassiveResourceAcquired.class.getCanonicalName(), PassiveResourceAcquired.class);
			foo.put(PassiveResourceReleased.class.getCanonicalName(), PassiveResourceReleased.class);
			foo.put(PassiveResourceStateUpdated.class.getCanonicalName(), PassiveResourceStateUpdated.class);
			foo.put(ResourceDemandRequestAborted.class.getCanonicalName(), ResourceDemandRequestAborted.class);
			foo.put(ResourceDemandRequested.class.getCanonicalName(), ResourceDemandRequested.class);
			foo.put(ResourceRequestFailed.class.getCanonicalName(), ResourceRequestFailed.class);
			foo.put(SEFFChildInterpretationStarted.class.getCanonicalName(), SEFFChildInterpretationStarted.class);
			foo.put(SEFFInterpretationFinished.class.getCanonicalName(), SEFFInterpretationFinished.class);
			foo.put(SEFFInterpretationProgressed.class.getCanonicalName(), SEFFInterpretationProgressed.class);
			foo.put(ClosedWorkloadUserInitiated.class.getCanonicalName(), ClosedWorkloadUserInitiated.class);
			foo.put(InnerScenarioBehaviorInitiated.class.getCanonicalName(), InnerScenarioBehaviorInitiated.class);
			foo.put(InterArrivalUserInitiated.class.getCanonicalName(), InterArrivalUserInitiated.class);
			foo.put(UsageScenarioFinished.class.getCanonicalName(), UsageScenarioFinished.class);
			foo.put(UsageScenarioStarted.class.getCanonicalName(), UsageScenarioStarted.class);
			foo.put(UserAborted.class.getCanonicalName(), UserAborted.class);
			foo.put(UserFinished.class.getCanonicalName(), UserFinished.class);
			foo.put(UserSlept.class.getCanonicalName(), UserSlept.class);
			foo.put(UserStarted.class.getCanonicalName(), UserStarted.class);
			foo.put(UserWokeUp.class.getCanonicalName(), UserWokeUp.class);
			foo.put(JobScheduled.class.getCanonicalName(), JobScheduled.class);
			foo.put(MeasurementMade.class.getCanonicalName(), MeasurementMade.class);
			foo.put(MeasurementUpdated.class.getCanonicalName(), MeasurementUpdated.class);
			foo.put(ProbeTaken.class.getCanonicalName(), ProbeTaken.class);
			foo.put(SEFFExternalActionCalled.class.getCanonicalName(), SEFFExternalActionCalled.class);
			foo.put(SEFFInfrastructureCalled.class.getCanonicalName(), SEFFInfrastructureCalled.class);
			foo.put(SnapshotFinished.class.getCanonicalName(), SnapshotFinished.class);
			foo.put(UserEntryRequested.class.getCanonicalName(), UserEntryRequested.class);
			foo.put(UserInterpretationProgressed.class.getCanonicalName(), UserInterpretationProgressed.class);
			foo.put(UserRequestFinished.class.getCanonicalName(), UserRequestFinished.class);
			foo.put(IntervalPassed.class.getCanonicalName(), IntervalPassed.class);
			foo.put(SimulationTimeReached.class.getCanonicalName(), SimulationTimeReached.class);

//			foo.put(ModelAdjusted.class.getCanonicalName(), ModelAdjusted.class);
//			foo.put(ModelAdjustmentRequested.class.getCanonicalName(), ModelAdjustmentRequested.class);
//			foo.put(PreSimulationConfigurationStarted.class.getCanonicalName(),
//					PreSimulationConfigurationStarted.class);
//			foo.put(ProcessingTypeRevealed.class.getCanonicalName(), ProcessingTypeRevealed.class);
//			foo.put(SimulationFinished.class.getCanonicalName(), SimulationFinished.class);
//			foo.put(SimulationStarted.class.getCanonicalName(), SimulationStarted.class);
//			foo.put(SnapshotInitiated.class.getCanonicalName(), SnapshotInitiated.class);
//			foo.put(SnapshotTaken.class.getCanonicalName(), SnapshotTaken.class);
//			foo.put(SPDAdjustorStateInitialized.class.getCanonicalName(), SPDAdjustorStateInitialized.class);
//			foo.put(TakeCostMeasurement.class.getCanonicalName(), TakeCostMeasurement.class);
//			foo.put(MonitorModelVisited.class.getCanonicalName(), MonitorModelVisited.class);
//			foo.put(MeasurementSpecificationVisited.class.getCanonicalName(), MeasurementSpecificationVisited.class);
//			foo.put(ProcessingTypeVisited.class.getCanonicalName(), ProcessingTypeVisited.class);
//			foo.put(RepositoryInterpretationInitiated.class.getCanonicalName(),
//			RepositoryInterpretationInitiated.class);
// 			foo.put(CalculatorRegistered.class.getCanonicalName(), CalculatorRegistered.class);

			return foo;
		}

	}

	@Override
	public void addEvent(final DESEvent event) {
		this.additionalEvents.add(event);
	}
}
