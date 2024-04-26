package org.palladiosimulator.analyzer.slingshot.stateexploration.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.ExplorationConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.PlannerWorkflowConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.jobs.InitialPlannerJob;
import org.palladiosimulator.analyzer.workflow.jobs.PreparePCMBlackboardPartitionJob;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.Repository.Repository;
import org.palladiosimulator.edp2.models.Repository.RepositoryFactory;
import org.palladiosimulator.experimentautomation.abstractsimulation.AbstractSimulationConfiguration;
import org.palladiosimulator.experimentautomation.abstractsimulation.AbstractsimulationPackage;
import org.palladiosimulator.experimentautomation.abstractsimulation.MeasurementCountStopCondition;
import org.palladiosimulator.experimentautomation.abstractsimulation.SimTimeStopCondition;
import org.palladiosimulator.experimentautomation.abstractsimulation.StopCondition;
import org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration;
import org.palladiosimulator.experimentautomation.experiments.Experiment;
import org.palladiosimulator.experimentautomation.experiments.ExperimentRepository;
import org.palladiosimulator.experimentautomation.experiments.ExperimentsPackage;
import org.palladiosimulator.recorderframework.edp2.config.EDP2RecorderConfigurationFactory;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.simulation.AbstractSimulationConfig;
import de.uka.ipd.sdq.workflow.BlackboardBasedWorkflow;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * In the field "Program arguments", provide an additional argument as shown in
 * this example:
 * <code>-os ${target.os} -ws ${target.ws} -arch ${target.arch} -nl ${target.nl}
 * -consoleLog D:\models\my.experiments</code>
 *
 *
 * @author stiesssh
 *
 */
public class StateexplorationApplication implements IApplication {

	private final String SLINGSHOT_ID = "org.palladiosimulator.slingshot";

	/**
	 * An experiment with at least on {@link StateExplorationConfiguration} tool
	 * configuration.
	 */
	private Experiment experiment;

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		System.out.println("plugin started");

		// parse commandline arguments, copied from ExperimentApplication
		// obtain command line arguments
		final String[] args = (String[]) context.getArguments().get("application.args");

		// check arguments
		if (args.length < 1) {
			System.out.println("The mandatory parameters have not been specified.");
			return IApplication.EXIT_OK;
		}

		// get location of experiments file
		final Path experimentsLocation = new Path(args[0]);

		// prepare filtered experiment list, if parameter has been specified
		final List<String> filteredExperimentIDs = new ArrayList<String>();
		if (args.length >= 2) {
			final String[] ids = args[1].split(";");
			for (final String id : ids) {
				filteredExperimentIDs.add(id);
			}
		}

		final List<Experiment> experiments = getExperiments(experimentsLocation, filteredExperimentIDs);


		final Optional<Experiment> experiment = experiments.stream().filter(e -> e.getToolConfiguration().stream()
				.filter(StateExplorationConfiguration.class::isInstance).findFirst().isPresent()).findFirst();

		if (experiment.isEmpty()) {
			throw new IllegalArgumentException(
					"No Experiment with tool configuration of type StateExploration(Simulation)Configuration. Cannot start exploration.");
		}

		this.experiment = experiment.get();

		launchPlannerSimulation();


		return IApplication.EXIT_OK;

	}


	private void launchPlannerSimulation()
			throws CoreException, JobFailedException, UserCanceledException {

		final HelloWorldJob job = new HelloWorldJob("World");

		// AbstractSimulationConfigFactory

		final Map<String, Object> configMap = createConfigMap(SLINGSHOT_ID);

		final SimuComConfig simuComconfig = new SimuComConfig(configMap, false);
		final PlannerWorkflowConfiguration config = new PlannerWorkflowConfiguration(simuComconfig, configMap);


		final BlackboardBasedWorkflow<MDSDBlackboard> workflow = new BlackboardBasedWorkflow<MDSDBlackboard>(job,
				new MDSDBlackboard());

		workflow.add(new PreparePCMBlackboardPartitionJob());
		workflow.add(new SetModelsInBlackboardJob(this.experiment.getInitialModel(), true));
		//workflow.add(new SimulationJob(config.getSimuComConfig()));
		workflow.add(new InitialPlannerJob(config));

		workflow.execute(new NullProgressMonitor());

	}



	@Override
	public void stop() {
		// Add operations when your plugin is stopped
	}

	/**
	 *
	 *
	 *
	 * @param simulatorID
	 * @return
	 */
	public Map<String, Object> createConfigMap(final String simulatorID) {
		final Map<String, Object> map = new HashMap<String, Object>();

		final String string= "TODO fix me";


		// we can do this because we already checked before.
		final StateExplorationConfiguration simConfig =
				(StateExplorationConfiguration) this.experiment.getToolConfiguration().stream().filter(StateExplorationConfiguration.class::isInstance).findFirst().get();

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

		// i can probably just ignore them, because the state exploration ignores them
		// as well.
		/** Stop Conditions */
		map.put(AbstractSimulationConfig.SIMULATION_TIME, this.getMaximumSimulationTime(experiment, simConfig));
		map.put(AbstractSimulationConfig.MAXIMUM_MEASUREMENT_COUNT,
				this.getMaximumMeasurementCount(experiment, simConfig));

		/** Logging */
		map.put(AbstractSimulationConfig.VERBOSE_LOGGING, false);

		/***************************************************/
		/** Analysis Configuration Tab *********************/
		/***************************************************/
		/** Random Number Generator Seed */
		map.put(AbstractSimulationConfig.USE_FIXED_SEED, false);

		map.put(ExplorationConfiguration.MAX_EXPLORATION_CYCLES, simConfig.getMaxIterations());
		map.put(ExplorationConfiguration.MIN_STATE_DURATION, simConfig.getMinStateDuration());


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

	private static List<Experiment> getExperiments(final IPath experimentsLocation,
			final List<String> filteredExperimentIDs) {
		final ResourceSet resourceSet = new ResourceSetImpl();
		final EClass expectedType = ExperimentsPackage.eINSTANCE.getExperimentRepository();
		final ExperimentRepository experimentRepository = (ExperimentRepository) loadResourceFromBundle(
				resourceSet, experimentsLocation, expectedType);

		final List<Experiment> experiments;
		if (filteredExperimentIDs == null || filteredExperimentIDs.isEmpty()) {
			// experiments as in config
			experiments = experimentRepository.getExperiments();
		} else {
			// filter experiment list
			experiments = new ArrayList<Experiment>();
			for (final Experiment e : experimentRepository.getExperiments()) {
				for (final String id : filteredExperimentIDs) {
					if (e.getId().equalsIgnoreCase(id)) {
						experiments.add(e);
						break;
					}
				}
			}
		}

		return experiments;
	}

	public String computeExperimentGroupPurpose() {
		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(experiment.getName());
		stringBuilder.append(" [");
		stringBuilder.append(experiment.getId());
		stringBuilder.append("]");

		return stringBuilder.toString();
	}

	private int getMaximumSimulationTime(final Experiment experiment,
			final AbstractSimulationConfiguration simConfig) {

		int result = getMaximumSimulationTime(experiment.getStopConditions());
		if (result == -1) { // Stick to defaults
			result = getMaximumSimulationTime(simConfig.getStopConditions());
		}

		return result;
	}

	private int getMaximumSimulationTime(final List<StopCondition> stopConditions) {
		for (final StopCondition s : stopConditions) {
			if (AbstractsimulationPackage.eINSTANCE.getSimTimeStopCondition().isInstance(s)) {
				return ((SimTimeStopCondition) s).getSimulationTime();
			}
		}

		// -1 causes the simulation to simulate indefinitely with regard to the
		// simulation time
		return -1;
	}

	private int getMaximumMeasurementCount(final Experiment experiment,
			final AbstractSimulationConfiguration simConfig) {
		int result = getMaximumMeasurementCount(experiment.getStopConditions());
		if (result == -1) { // Stick to defaults
			result = getMaximumMeasurementCount(simConfig.getStopConditions());
		}

		return result;
	}

	private int getMaximumMeasurementCount(final List<StopCondition> stopConditions) {
		for (final StopCondition s : stopConditions) {
			if (AbstractsimulationPackage.eINSTANCE.getMeasurementCountStopCondition().isInstance(s)) {
				return ((MeasurementCountStopCondition) s).getMeasurementCount();
			}
		}

		// -1 causes the simulation to simulate indefinitely with regard to the
		// measurement count
		return -1;
	}

	//	private static String getPersistenceRecorder(final EDP2Datasource datasource) {
	//		final Repository repository = EDP2DatasourceFactory.createOrOpenDatasource(datasource);
	//		return repository.getId();
	//	}

	public static <T extends EClass> EObject loadResourceFromBundle(final ResourceSet resourceSet,
			final IPath modelLocation, final T expectedType) {
		System.out.println("Loading resource " + modelLocation.toString() + " from bundle");
		final URI modelUri = URI.createFileURI(modelLocation.toOSString()); // absolutePathToBundleURI(bundle,
		// modelLocation);
		final Resource r = resourceSet.getResource(modelUri, true);

		final EObject o = r.getContents().get(0);
		if (expectedType.isInstance(o)) {
			return o;
		} else {
			throw new RuntimeException("The root element of the loaded resource is not of the expected type "
					+ expectedType.getName());
		}
	}
}