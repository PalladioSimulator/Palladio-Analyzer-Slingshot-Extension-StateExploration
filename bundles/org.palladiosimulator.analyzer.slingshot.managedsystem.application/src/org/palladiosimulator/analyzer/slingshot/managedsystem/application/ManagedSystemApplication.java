package org.palladiosimulator.analyzer.slingshot.managedsystem.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;
import org.palladiosimulator.analyzer.slingshot.workflow.SimulationWorkflowConfiguration;
import org.palladiosimulator.analyzer.slingshot.workflow.jobs.SimulationRootJob;
import org.palladiosimulator.experimentautomation.application.ExperimentApplication;
import org.palladiosimulator.experimentautomation.application.tooladapter.abstractsimulation.AbstractSimulationConfigFactory;
import org.palladiosimulator.experimentautomation.application.tooladapter.slingshot.model.SlingshotConfiguration;
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
 * Application for running a headless Managed System. Requires an {@link Experiment} model instance,
 * that defines the models et cetera.
 *
 * The path to the {@link Experiment} model instance must be provided as first commandline argument.
 * If needed, the UUID for the clientId can be provided as second commandline argument (as String).
 *
 * For OSGi runs inside Eclipse, supply the path a additional argument in the field "Program
 * arguments".
 *
 * As Example:
 *
 * -application <Application name> /path/to/model.experiments aaaabbbb-cccc-dddd-eeee-ffff11112222
 *
 * Based on {@link ExperimentApplication}.
 *
 * @author Sophie Stieß
 *
 */
public class ManagedSystemApplication implements IApplication {

    private final String MANAGED_SYSTEM_ID = "org.palladiosimulator.managedsystem";


	@Override
	public Object start(final IApplicationContext context) throws Exception {

        final Path experimentsLocation = parseCommandlineArguments(context);
		final Experiment experiment = getStateExplorationExperiment(experimentsLocation).orElseThrow(() -> new IllegalArgumentException(
                "No Experiment with tool configuration of type SlingshotConfiguration. Cannot start simulation."));

        final Optional<String> clientId = parseClientId(context);
        clientId.ifPresent(id -> EventMessage.CLIENT_ID = UUID.fromString(id));

		launchStateExploration(experiment);


		return IApplication.EXIT_OK;
	}

	/**
     * Get parse first command line argument and parse it to a path.
     *
     * @param context
     *            to parse the arguments from. Must have at least one argument.
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
     * Get parse first command line argument and parse it to a path.
     *
     * @param context
     *            to parse the arguments from. Must have at least one argument.
     * @return first command line argument as Path.
     */
    private Optional<String> parseClientId(final IApplicationContext context) {
        final String[] args = (String[]) context.getArguments()
            .get("application.args");

        if (args.length < 2) {
            return Optional.empty();
        }

        return Optional.of(args[1]);
    }

	/**
     * Get an experiment with a {@link SlingshotConfiguration}.
     *
     * @param modelLocation
     *            path the the *.experiments file
     * @return first experiment with a {@link SlingshotConfiguration} or {@link Optional#empty()} if
     *         none exists.
     */
	private Optional<Experiment> getStateExplorationExperiment(final IPath modelLocation) {

		final List<Experiment> experiments = loadExperimentsFromFile(modelLocation);

		return experiments.stream().filter(e -> e.getToolConfiguration().stream()
                .filter(SlingshotConfiguration.class::isInstance)
                .findFirst()
                .isPresent())
            .findFirst();
	}

	/**
     *
     * Create and execute a workflow for preparing and running the managed system.
     *
     * @param experiment
     */
	private void launchStateExploration(final Experiment experiment) {

        final Map<String, Object> configMap = createConfigMap(experiment, MANAGED_SYSTEM_ID);

		final SimuComConfig simuComconfig = new SimuComConfig(configMap, false);
        final SimulationWorkflowConfiguration config = new SimulationWorkflowConfiguration(simuComconfig);

		this.setModelFilesInConfig(experiment.getInitialModel(), config);

		final BlackboardBasedWorkflow<MDSDBlackboard> workflow = new BlackboardBasedWorkflow<MDSDBlackboard>(
                new SimulationRootJob(config, null),
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
    private void setModelFilesInConfig(final InitialModel models, final SimulationWorkflowConfiguration config) {
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

        final SlingshotConfiguration simConfig = (SlingshotConfiguration) experiment.getToolConfiguration()
            .stream()
            .filter(SlingshotConfiguration.class::isInstance)
            .findFirst()
            .get();

		final Map<String, Object> map = AbstractSimulationConfigFactory.createConfigMap(experiment, simConfig,
				simulatorID,
				List.of());

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