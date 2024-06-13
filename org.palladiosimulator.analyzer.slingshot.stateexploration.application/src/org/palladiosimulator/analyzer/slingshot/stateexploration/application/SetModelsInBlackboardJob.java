package org.palladiosimulator.analyzer.slingshot.stateexploration.application;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.experimentautomation.experiments.InitialModel;

import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.SequentialBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.ResourceSetPartition;

/**
 * Stripped down version of
 * {@link org.palladiosimulator.experimentautomation.application.jobs.LoadModelsIntoBlackboardJob}
 *
 * @author Sarah Stieß
 */
public class SetModelsInBlackboardJob extends SequentialBlackboardInteractingJob<MDSDBlackboard> {

	private static final Logger LOGGER = Logger.getLogger(SetModelsInBlackboardJob.class);

	private final InitialModel initialModel;

	public SetModelsInBlackboardJob(final InitialModel initialModel) {
		super(false);
		this.initialModel = initialModel;
	}

	@Override
	public void execute(final IProgressMonitor monitor) throws JobFailedException, UserCanceledException {
		// Load the PCM models
		final List<EObject> pcmModels = new ArrayList<EObject>();

		pcmModels.add(this.initialModel.getRepository());
		pcmModels.add(this.initialModel.getSystem());
		pcmModels.add(this.initialModel.getResourceEnvironment());
		pcmModels.add(this.initialModel.getAllocation());
		pcmModels.add(this.initialModel.getUsageModel());
		pcmModels.add(this.initialModel.getMiddlewareRepository());
		pcmModels.add(this.initialModel.getEventMiddleWareRepository());

		pcmModels.add(this.initialModel.getScalingDefinitions());
		pcmModels.add(this.initialModel.getSpdSemanticConfiguration());
		pcmModels.add(this.initialModel.getUsageEvolution());
		pcmModels.add(this.initialModel.getMonitorRepository());
		pcmModels.add(this.initialModel.getServiceLevelObjectives());

		// load the PCM model into the standard partition
		loadIntoBlackboard(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID, pcmModels);
	}

	private void loadIntoBlackboard(final String partitionId, final List<EObject> eObjects) {
		final ResourceSetPartition partition = this.getBlackboard().getPartition(partitionId);
		if (LOGGER.isEnabledFor(Level.INFO)) {
			LOGGER.info("Loading models for partition " + partitionId);
		}
		for (final EObject eObject : eObjects) {
			loadIfExisting(partition, eObject);
		}
		partition.resolveAllProxies();
	}

	private static void loadIfExisting(final ResourceSetPartition resourceSetPartition, final EObject eObject) {
		if (eObject != null && eObject.eResource() != null) {
			if (LOGGER.isEnabledFor(Level.INFO)) {
				LOGGER.info("Load " + eObject.eResource().getURI());
			}
			resourceSetPartition.loadModel(eObject.eResource().getURI());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "Get Initial Models into Blackboard";
	}
}
