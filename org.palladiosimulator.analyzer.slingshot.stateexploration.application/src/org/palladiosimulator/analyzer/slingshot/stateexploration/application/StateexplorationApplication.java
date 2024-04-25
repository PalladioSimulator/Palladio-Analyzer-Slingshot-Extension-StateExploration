package org.palladiosimulator.analyzer.slingshot.stateexploration.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.palladiosimulator.analyzer.slingshot.workflow.SimulationWorkflowConfiguration;
import org.palladiosimulator.analyzer.slingshot.workflow.jobs.SimulationRootJob;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.Repository.Repository;
import org.palladiosimulator.edp2.models.Repository.RepositoryFactory;
import org.palladiosimulator.recorderframework.edp2.config.EDP2RecorderConfigurationFactory;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.simulation.AbstractSimulationConfig;
import de.uka.ipd.sdq.workflow.BlackboardBasedWorkflow;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

public class StateexplorationApplication implements IApplication {

	private final String SLINGSHOT_ID = "org.palladiosimulator.slingshot";

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		System.out.println("plugin started");

		launchPlannerSimulation();

		return IApplication.EXIT_OK;

	}


	private void launchPlannerSimulation() throws CoreException, JobFailedException, UserCanceledException {

		final HelloWorldJob job = new HelloWorldJob("World");
		job.execute(new NullProgressMonitor());

		// AbstractSimulationConfigFactory

		final SimuComConfig simuComconfig = new SimuComConfig(createConfigMap(SLINGSHOT_ID), false);
		final SimulationWorkflowConfiguration config = new SimulationWorkflowConfiguration(simuComconfig);

		config.setAllocationFiles(
				List.of("file:/Users/stiesssh/uni/repos/slingshot-example/minimalexample/default.allocation"));
		config.setUsageModelFile("file:/Users/stiesssh/uni/repos/slingshot-example/minimalexample/default.usagemodel");

		final SimulationRootJob simjob = new SimulationRootJob(config, null);

		final BlackboardBasedWorkflow<MDSDBlackboard> workflow = new BlackboardBasedWorkflow<MDSDBlackboard>(simjob,
				new MDSDBlackboard());
		workflow.execute(new NullProgressMonitor());

		System.out.println("Planner Simulation launched");
	}


	@Override
	public void stop() {
		// Add operations when your plugin is stopped
	}

	public static Map<String, Object> createConfigMap(final String simulatorID) {
		final Map<String, Object> map = new HashMap<String, Object>();

		final String string= "TODO fix me";

		/***************************************************/
		/** Simulation Tab *********************************/
		/***************************************************/
		/** Simulator */
		map.put(AbstractSimulationConfig.SIMULATOR_ID, simulatorID);

		/** Failure simulation */
		map.put(SimuComConfig.SIMULATE_FAILURES, false);

		/** Experiment Run */
		map.put(AbstractSimulationConfig.EXPERIMENT_RUN, string);
		map.put(EDP2RecorderConfigurationFactory.VARIATION_ID, string);

		/** Simulation Results */
		map.put(AbstractSimulationConfig.PERSISTENCE_RECORDER_NAME,
				org.palladiosimulator.recorderframework.edp2.Activator.EDP2_ID);
		map.put(EDP2RecorderConfigurationFactory.REPOSITORY_ID, getPersistenceRecorder());

		/** Stop Conditions */
		map.put(AbstractSimulationConfig.SIMULATION_TIME, 109);
		map.put(AbstractSimulationConfig.MAXIMUM_MEASUREMENT_COUNT, 0);

		/** Logging */
		map.put(AbstractSimulationConfig.VERBOSE_LOGGING, false);

		/***************************************************/
		/** Analysis Configuration Tab *********************/
		/***************************************************/
		/** Random Number Generator Seed */
		map.put(AbstractSimulationConfig.USE_FIXED_SEED, false);

		// the class SimuComConfig expects map entries to have a value of type String
		adjustMapValueTypes(map);

		return map;
	}

	/**
	 * TODO Check whether this method is actually still needed. [Lehrig]
	 *
	 * Converts the values contained in the map to the data types that are expected
	 * by {@link AbstractSimulationConfig}.
	 *
	 * @param map the attributes map for a run configuration.
	 */
	private static void adjustMapValueTypes(final Map<String, Object> map) {
		for (final Entry<String, Object> entry : map.entrySet()) {
			final Object value = entry.getValue();

			// As an exception, Booleans are not represented by string
			if (!(value instanceof Boolean)) {
				entry.setValue(value.toString());
			}
		}
	}

	private static String getPersistenceRecorder() {
		final Repository repository = RepositoryFactory.eINSTANCE.createLocalMemoryRepository();
		RepositoryManager.addRepository(RepositoryManager.getCentralRepository(), repository);
		return repository.getId();
	}
}