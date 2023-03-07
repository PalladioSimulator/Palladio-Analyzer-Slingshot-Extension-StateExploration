package org.palladiosimulator.analyzer.slingshot.workflow.planner.launcher.jobs;

import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.palladiosimulator.analyzer.slingshot.simulation.core.SlingshotModel;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.GraphExplorer;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawStateGraph;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.RawTransition;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.DefaultGraphExplorer;
import org.palladiosimulator.analyzer.slingshot.workflow.planner.configuration.SimulationWorkflowConfiguration;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.SpdPackage;

import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * This class is responsible for starting the planner.
 *
 * @author stiesssh
 */
public class InitialPlannerJob implements IBlackboardInteractingJob<MDSDBlackboard> {

	private static final Logger LOGGER = Logger.getLogger(InitialPlannerJob.class.getName());

	private MDSDBlackboard blackboard;

	private final SimulationWorkflowConfiguration configuration;

	public InitialPlannerJob(final SimulationWorkflowConfiguration config) {
		this.configuration = config;
	}

	@Override
	public void execute(final IProgressMonitor monitor) throws JobFailedException, UserCanceledException {
		LOGGER.info("**** SimulationJob.execute ****");

		final SlingshotModel model = this.loadModelFromBlackboard();


		// i'd prefer to have the actual GraphExplorer Implementation hidden and provided via Injection, but am not yet sufficiently proficient with the Injection Mechanism. Will probably only fix this in the new framework.
		final GraphExplorer explorer = new DefaultGraphExplorer(model, this.blackboard, configuration.getlaunchConfigParams());

		final RawStateGraph rawGraph =  explorer.start();
		// return the raw graph


		final Set<RawTransition> transitions = rawGraph.getRoot().getOutTransitions();

		for (final RawTransition transition : transitions) {
			//transition.get...
		}




		LOGGER.info("**** SimulationJob.execute  - Done ****");
	}

	private SlingshotModel loadModelFromBlackboard() {
		final PCMResourceSetPartition partition = (PCMResourceSetPartition) this.blackboard
				.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);
		final SlingshotModel model = SlingshotModel.builder().withAllocationModel(partition.getAllocation())
				.withUsageModel(partition.getUsageModel())
				.withMonitorinRepositoryFile((MonitorRepository) partition
						.getElement(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository()).get(0))
				.withSpdFile((SPD) partition.getElement(SpdPackage.eINSTANCE.getSPD()).get(0)).build();
		return model;
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
