package org.palladiosimulator.analyzer.slingshot.stateexploration.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.ExplorationConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.ExplorationWorkflowConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.workflow.jobs.ExplorationRootJob;
import org.palladiosimulator.experimentautomation.application.ExperimentApplication;
import org.palladiosimulator.experimentautomation.application.tooladapter.abstractsimulation.AbstractSimulationConfigFactory;
import org.palladiosimulator.experimentautomation.application.tooladapter.stateexploration.model.StateExplorationConfiguration;
import org.palladiosimulator.experimentautomation.experiments.Experiment;
import org.palladiosimulator.experimentautomation.experiments.ExperimentRepository;
import org.palladiosimulator.experimentautomation.experiments.ExperimentsPackage;
import org.palladiosimulator.experimentautomation.experiments.InitialModel;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.BlackboardBasedWorkflow;
import de.uka.ipd.sdq.workflow.WorkflowFailedException;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 *
 * Application for running a headless Stateexploration. Requires an
 * {@link Experiment} model instance, that defines the models et cetera.
 *
 * The path to the {@link Experiment} model instance must be provided as
 * commandline argument.
 *
 * For OSGi runs inside Eclipse, supply the path a additional argument in the
 * field "Program arguments".
 *
 * Based on {@link ExperimentApplication}.
 *
 * @author Sarah Stieß
 *
 */
public class StateexplorationApplication implements IApplication {

	private final String STATE_EXPLORATION_ID = "org.palladiosimulator.stateexploration";


	@Override
	public Object start(final IApplicationContext context) throws Exception {
		final Path experimentsLocation = parseCommandlineArguments(context);

		final Experiment experiment = getStateExplorationExperiment(experimentsLocation).orElseThrow(() -> new IllegalArgumentException(
				"No Experiment with tool configuration of type StateExploration(Simulation)Configuration. Cannot start exploration."));

		launchStateExploration(experiment);

		return IApplication.EXIT_OK;
	}

	/**
	 * Get command line arguments and parse them.
	 *
	 * @param context to parse the arguments from. Must have at least one argument.
	 * @return first command line argument as Path.
	 */
	private Path parseCommandlineArguments(final IApplicationContext context) {
		final String[] args = (String[]) context.getArguments().get("application.args");

		if (args.length < 1) {
			throw new IllegalArgumentException("The mandatory argument is missing.");
		}

		return new Path(args[0]);
	}

	/**
	 * Get an experiment with a {@code StateExplorationConfiguration}.
	 *
	 * @param modelLocation path the the *.experiments file
	 * @return first experiment with a {@code StateExplorationConfiguration} or
	 *         {@link Optional#empty()} if none exists.
	 */
	private Optional<Experiment> getStateExplorationExperiment(final IPath modelLocation) {

		final List<Experiment> experiments = loadExperimentsFromFile(modelLocation);

		return experiments.stream().filter(e -> e.getToolConfiguration().stream()
				.filter(StateExplorationConfiguration.class::isInstance).findFirst().isPresent()).findFirst();
	}

	/**
	 *
	 * Create and execute a workflow for preparing and running a state exploration.
	 *
	 * @param experiment
	 */
	private void launchStateExploration(final Experiment experiment) {

		final Map<String, Object> configMap = createConfigMap(experiment, STATE_EXPLORATION_ID);

		final SimuComConfig simuComconfig = new SimuComConfig(configMap, false);
		final ExplorationWorkflowConfiguration config = new ExplorationWorkflowConfiguration(simuComconfig, configMap);

		this.setModelFilesInConfig(experiment.getInitialModel(), config);

		final BlackboardBasedWorkflow<MDSDBlackboard> workflow = new BlackboardBasedWorkflow<MDSDBlackboard>(
				new ExplorationRootJob(config, null),
				new MDSDBlackboard());

		try {
			workflow.execute(new NullProgressMonitor());
		} catch (JobFailedException | UserCanceledException e) {
			throw new WorkflowFailedException("Workflow failed", e);
		}
	}

	/**
	 * Get the file location of the initial models, and put the into the
	 * {@code config}.
	 *
	 * This is necessary, to use the {@link ExplorationRootJob}, which load the
	 * model as defined in the {@link ExplorationWorkflowConfiguration}.
	 *
	 * @param models initial models, as defined in the experiments file.
	 * @param config configuration to start the exploration on.
	 */
	private void setModelFilesInConfig(final InitialModel models, final ExplorationWorkflowConfiguration config) {
		this.consumeModelLocation(models.getAllocation(), s -> config.setAllocationFiles(List.of(s)));
		this.consumeModelLocation(models.getUsageModel(), s -> config.setUsageModelFile(s));

		this.consumeModelLocation(models.getScalingDefinitions(), s -> config.addOtherModelFile(s));
		this.consumeModelLocation(models.getSpdSemanticConfiguration(), s -> config.addOtherModelFile(s));
		this.consumeModelLocation(models.getMonitorRepository(), s -> config.addOtherModelFile(s));
		this.consumeModelLocation(models.getServiceLevelObjectives(), s -> config.addOtherModelFile(s));
		this.consumeModelLocation(models.getUsageEvolution(), s -> config.addOtherModelFile(s));
	}

	/**
	 *
	 * Make {@code consumer} accept the URI of {@code model}, if it is not
	 * {@code null}.
	 *
	 * If {@code model} is {@code null}, nothing happens.
	 *
	 * @param model    model who's URI will be consumed. May be {@code null}.
	 * @param consumer consumer to consume the model's URI.
	 */
	private void consumeModelLocation(final EObject model, final Consumer<String> consumer) {
		assert consumer != null;
		if (model == null) {
			return;
		}
		consumer.accept(model.eResource().getURI().toString());
	}

	@Override
	public void stop() {
		// Add operations when your plugin is stopped
	}

	/**
	 * Create map with configuration for the {@link SimuComConfig}.
	 *
	 * Uses the factory from the experiment automation and adds the exploration
	 * specific configurations afterwards.
	 *
	 * Beware: For some reason, all values but booleans are expected to be of type
	 * {@link String}.
	 *
	 * @param experiment  input for creating the configuration map
	 * @param simulatorID id of the simulator
	 * @return configurations to create the {@link SimuComConfig}.
	 */
	public Map<String, Object> createConfigMap(final Experiment experiment, final String simulatorID) {

		final StateExplorationConfiguration simConfig =
				(StateExplorationConfiguration) experiment.getToolConfiguration().stream()
				.filter(StateExplorationConfiguration.class::isInstance).findFirst().get();

		final Map<String, Object> map = AbstractSimulationConfigFactory.createConfigMap(experiment, simConfig,
				simulatorID,
				List.of());

		map.put(ExplorationConfiguration.MAX_EXPLORATION_CYCLES, String.valueOf(simConfig.getMaxIterations()));
		map.put(ExplorationConfiguration.MIN_STATE_DURATION, String.valueOf(simConfig.getMinStateDuration()));
		map.put(ExplorationConfiguration.SENSIBILITY, String.valueOf(simConfig.getSensitivity()));
		map.put(ExplorationConfiguration.IDLE_EXPLORATION, String.valueOf(simConfig.isDoIdleExploration()));
		map.put(ExplorationConfiguration.MODEL_LOCATION, String.valueOf(simConfig.getModeLocation()));

		return map;
	}

	/**
	 * Loads an experiments model from the given location.
	 *
	 * @param modelLocation path the the *.experiments file
	 * @return list of experiments
	 */
	private static List<Experiment> loadExperimentsFromFile(final IPath modelLocation) {
		System.out.println("Loading resource " + modelLocation.toString() + " from bundle");
		final URI modelUri = URI.createFileURI(modelLocation.toOSString());
		final Resource r = (new ResourceSetImpl()).getResource(modelUri, true);

		final EObject o = r.getContents().get(0);
		if (ExperimentsPackage.eINSTANCE.getExperimentRepository().isInstance(o)) {
			return ((ExperimentRepository) o).getExperiments();
		} else {
			throw new RuntimeException("The root element of the loaded resource is not of the expected type "
					+ ExperimentsPackage.eINSTANCE.getExperimentRepository().getName());
		}
	}
}