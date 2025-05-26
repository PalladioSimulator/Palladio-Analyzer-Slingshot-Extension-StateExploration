package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.configuration;

import java.nio.file.Path;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.NewArchThing;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.SingleStateSimulationExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.SingleStateSimulationExplorer.SimulationResult;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.AdditionalConfigurationModule;
import org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.InitStateDeSerialization;
import org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.OtherStuffDeserialization;
import org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.ResultStateSerialization;
import org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.data.InitState;
import org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.data.OtherInitThings;
import org.palladiosimulator.analyzer.slingshot.workflow.WorkflowConfigurationModule;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * This class is responsible for starting the explorer.
 *
 * @author Sarah Stieß
 */
public class RunSingleStateSimulationJob implements IBlackboardInteractingJob<MDSDBlackboard> {

	private static final Logger LOGGER = Logger.getLogger(RunSingleStateSimulationJob.class.getName());

	private MDSDBlackboard blackboard;
	private final PCMResourceSetPartitionProvider pcmResourceSetPartitionProvider;

	private final SingleStateSimulationWorkflowConfiguration configuration;
	
	public RunSingleStateSimulationJob(final SingleStateSimulationWorkflowConfiguration  config) {
		this.configuration = config;
		this.pcmResourceSetPartitionProvider = Slingshot.getInstance().getInstance(PCMResourceSetPartitionProvider.class);
	}

	@Override
	public void execute(final IProgressMonitor monitor) throws JobFailedException, UserCanceledException {

		LOGGER.info("**** SimulationJob.prepare ****");
		final PCMResourceSetPartition partition = (PCMResourceSetPartition)
				this.blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);
		
		WorkflowConfigurationModule.simuComConfigProvider.set(this.configuration.getSimuComConfig());
		WorkflowConfigurationModule.blackboardProvider.set(this.blackboard);

		this.pcmResourceSetPartitionProvider.set(partition);

		final URI resultFolder = URI.createFileURI(configuration.getResultsFolder().toString());
		NewArchThing.copyModelsForRoot(partition.getResourceSet(), resultFolder);

		LOGGER.debug("monitor: " + monitor.getClass().getName());
		monitor.beginTask("Start Simulation", 3);

		monitor.subTask("Initialize driver");
					
		// loading the snapshot
		final InitStateDeSerialization thing = new InitStateDeSerialization(partition);
		final InitState initState = thing.deserialize(configuration.getSnapshotFile());
		
		// loading other things 
		final OtherStuffDeserialization stuff = new OtherStuffDeserialization(partition);
		final OtherInitThings otherInitThings = stuff.deserialize(configuration.getOtherConfigsFile());
		
		final SnapshotConfiguration snaphshotConfig = new SnapshotConfiguration(
				this.configuration.getSimuComConfig().getSimuTime(), !otherInitThings.isRootSuccesor(), otherInitThings.getSensibility(),
				this.configuration.getSimuComConfig().getSimuTime());
		
		
		AdditionalConfigurationModule.snapConfigProvider.set(snaphshotConfig);
		

		// this.configuration.getlaunchConfigParams() --> access to all the params.
		
		// get explorere for single state and run the simulation //	
		final SingleStateSimulationExplorer explorer = new SingleStateSimulationExplorer(this.configuration.getSimuComConfig(), monitor, this.blackboard, initState.getSnapshot(), initState.getId());
		final SimulationResult result = explorer.simulateSingleState(otherInitThings.getIncomingPolicies(), initState.getPointInTime());

		final String fileName = configuration.getSnapshotFile().getFileName().toString();
		final Path snapPath = Path.of(configuration.getResultsFolder().toString(), fileName);
		
		thing.serialize(result.initState(), snapPath);
		

		final String resultName = "results.json";
		final Path resultPath = Path.of(configuration.getResultsFolder().toString(), resultName);
		
		// Serailize shit. 
		final ResultStateSerialization resultThing = new ResultStateSerialization();
		resultThing.serialize(result.state(), resultPath);

		
		monitor.worked(1);

		monitor.subTask("Start simulation");
		monitor.worked(1);

		monitor.subTask("Restore");
		monitor.worked(1);

		monitor.done();

		LOGGER.info("**** SimulationJob.execute  - Done ****");
	}

	@Override
	public void cleanup(final IProgressMonitor monitor) throws CleanupFailedException {

	}

	@Override
	public String getName() {
		return this.getClass().getCanonicalName();
	}

	@Override
	public void setBlackboard(final MDSDBlackboard blackboard) {
		this.blackboard = blackboard;
	}

}
