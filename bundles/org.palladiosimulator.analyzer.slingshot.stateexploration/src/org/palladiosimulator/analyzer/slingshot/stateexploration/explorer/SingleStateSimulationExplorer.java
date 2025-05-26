package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateInitialized;
import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.converter.MeasurementConverter;
import org.palladiosimulator.analyzer.slingshot.converter.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.converter.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.converter.data.Utility;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationDriver;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.Postprocessor;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning.SingleStateSimulationPreprocessor;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.ExploredState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.graph.ExploredStateBuilder;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.AdditionalConfigurationModule;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.EventsToInitOnWrapper;
import org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.data.InitState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.data.ResultState;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.mdsdprofiles.api.ProfileAPI;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentFactory;
import org.palladiosimulator.pcm.util.PcmResourceImpl;
import org.palladiosimulator.spd.ScalingPolicy;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * Core Component of the Exploration.
 *
 * Responsible for exploring new states.
 *
 * @author Sarah Stieß
 *
 */
public class SingleStateSimulationExplorer {

	private static final Logger LOGGER = Logger.getLogger(SingleStateSimulationExplorer.class.getName());

	/** content changes with each iteration */
	private final PCMResourceSetPartition initModels;

	private final SimuComConfig simuComConfig;

	private final SingleStateSimulationPreprocessor preprocessor;

	private final IProgressMonitor monitor;	
	private final String parentId;
	
	private final Snapshot snapshot;

	public SingleStateSimulationExplorer(final SimuComConfig config, final IProgressMonitor monitor, 
			final MDSDBlackboard blackboard, final Snapshot snapshot, final String parentId) {
		super();
		this.initModels = (PCMResourceSetPartition) blackboard
				.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);
		this.simuComConfig = config;
		this.monitor = monitor;
		this.parentId = parentId;

		applyStereotypeFake();

		//EcoreUtil.resolveAll(initModels.getResourceSet());
		
		this.snapshot = snapshot;

		this.preprocessor = new SingleStateSimulationPreprocessor(snapshot, blackboard);
	}



	public SimulationResult simulateSingleState(final List<ScalingPolicy> policies, final double startTime) {
		LOGGER.info("********** DefaultGraphExplorer.explore() **********");
//		
//		final SimulationInitConfiguration config = this.preprocessor.createConfigForNextSimualtionRun(policies, this.parentId, startTime);
		
		final ExploredStateBuilder stateBuilder = new ExploredStateBuilder(parentId, startTime);
		
		final Set<SPDAdjustorStateInitialized> stateInitEvents = this.preprocessor.createUpdateStateInitEvents(policies);
		final List<ModelAdjustmentRequested> initialAdjustments = this.preprocessor.createInitialModelAdjustmentRequested(policies);
				
		AdditionalConfigurationModule.defaultStateProvider.set(stateBuilder);
		AdditionalConfigurationModule.eventsToInitOnProvider.set(new EventsToInitOnWrapper(initialAdjustments, stateInitEvents, snapshot.getEvents()));
		

		final SimulationDriver driver = Slingshot.getInstance().getSimulationDriver();
		driver.init(simuComConfig, monitor);
		driver.start();
		
		return this.postProcessExplorationCycle(stateBuilder);
	}
	
	public record SimulationResult(ResultState state, InitState initState) {
		
	}

	/**
	 *
	 * Post {@link StateExploredEventMessage}, update fringe and write utility back
	 * to state.
	 *
	 * TODO : write Snapshot and measurements to file. need not write the models, because they alread are on file. 
	 *
	 * @param config configuration of exploration cycle to be post processed.
	 */
	private SimulationResult postProcessExplorationCycle(final ExploredStateBuilder stateBuilder) {

		final ExploredState current = stateBuilder.buildState();
		
		new Postprocessor(initModels).reduceTriggerTime(current.getDuration());
		
		final List<ScalingPolicy> policies = current.getSnapshot().getModelAdjustmentRequestedEvent().stream().map(e -> e.getScalingPolicy())
				.toList();

		final List<MeasurementSet> measurements = new MeasurementConverter(0.0, current.getDuration()).visitExperiementSetting(current.getExperimentSetting());

		final Utility utility = StateGraphNode.calcUtility(current.getStartTime(), current.getEndTime(), measurements, PCMResourcePartitionHelper.getSLORepository(initModels).getServicelevelobjectives());
		
		final ResultState resultState = new ResultState(initModels, current.getStartTime(), measurements, current.getDuration(), current.getReasonsToLeave(), current.getParentId(), policies, utility);
		
		return  new SimulationResult(resultState, new InitState(current.getEndTime(), current.getSnapshot(), current.getParentId()));
	}
	
	
	
	/**
	 * Applying a Profile and and the Stereotypes is necessary, because otherwise
	 * {@link EcoreUtil#resolveAll(org.eclipse.emf.ecore.resource.ResourceSet)}
	 * fails to resolve stereotype applications for cost.
	 * 
	 * This behaviour can also be observed in the PalladioBench UI. When opening a
	 * resource environment model with stereotypes and profile, we get an exception
	 * (PackageNotFound). If we create a new resource environment model, apply
	 * profiles and stereotypes to the new model, and only open the actual resource
	 * environment model afterwards, it opens just fine.
	 * 
	 * Thus, basically, this operation simulates what i have to do in the
	 * PalladioBench UI. I assume, that they fucked up the loading of profile
	 * models, but somehow the get loaded upon calling
	 * {@link ProfileAPI#applyProfile(Resource, String)} and
	 * {@link StereotypeAPI#applyStereotype(org.eclipse.emf.ecore.EObject, String)}.
	 * 
	 * <br>
	 * <b>Note</b>: Re-applying Profiles to the original model (which works in the
	 * UI) fails here, because re-application throws an error.
	 * 
	 * <br>
	 * <b>Note</b>: The resource and model created in this operation exist solely
	 * for getting the cost profile and stereotypes loaded. They have no other
	 * purpose and (probably) get garbage collect after this operation.
	 * 
	 * <br>
	 * <b>Note</b>: For the state exploration, we must do this <b>before</b> we
	 * create the root node. When creating the root node, we save a copy of the
	 * models to file, and all unresolved references go missing on save.
	 */
	private void applyStereotypeFake() {
		final ResourceEnvironment fake = ResourceenvironmentFactory.eINSTANCE.createResourceEnvironment();
		final Resource fakeRes = new PcmResourceImpl(URI.createFileURI(java.lang.System.getProperty("java.io.tmpdir")));
		fakeRes.getContents().add(fake);
		ProfileAPI.applyProfile(fakeRes, "Cost");
		StereotypeAPI.applyStereotype(fake, "CostReport");
	}

}
