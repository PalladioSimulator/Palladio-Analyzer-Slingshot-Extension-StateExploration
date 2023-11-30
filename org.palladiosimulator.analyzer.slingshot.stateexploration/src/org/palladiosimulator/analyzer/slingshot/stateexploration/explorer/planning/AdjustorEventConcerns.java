package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.AdjustorBasedEvent;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.StepBasedAdjustor;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.Workload;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.TargetGroup;
import de.uka.ipd.sdq.simucomframework.usage.ClosedWorkload;
import de.uka.ipd.sdq.simucomframework.usage.OpenWorkload;

/**
 * To be used during exploration planning.
 *
 * Responsible for things concerned with / related to {@link AdjustorBasedEvent}s.
 *
 * @author stiesssh
 *
 */
public class AdjustorEventConcerns {

	private static final Logger LOGGER = Logger.getLogger(AdjustorEventConcerns.class.getName());

	/**
	 * Create copy of the given event and update TargetGroup to reference the new copy of the architecture.
	 *
	 * @param event event to be copied
	 * @param config architecture to be referenced
	 * @return copy of event
	 */
	public DESEvent copyForTargetGroup(final DESEvent event, final ArchitectureConfiguration config){

		if (event instanceof final AdjustorBasedEvent adjustor) {
			final TargetGroup tg = adjustor.getTargetGroup();

			/* Update Target Group */
			if (tg instanceof final ElasticInfrastructure ei) {
				// TODO fix this
				// TODO search for Unit matching the unit of the given TG. 
				ei.setUnit(config.getAllocation().getTargetResourceEnvironment_Allocation().getResourceContainer_ResourceEnvironment().get(0));
				//ei.setPCM_ResourceEnvironment(config.getAllocation().getTargetResourceEnvironment_Allocation());
			} else {
				throw new IllegalArgumentException(String.format("Target Group of type %s not yet supported", tg.getClass().getSimpleName()));
			}

			/*Create Event copy*/
			if (event instanceof final StepBasedAdjustor specificAdjustor) {
				return new StepBasedAdjustor(tg, specificAdjustor.getStepCount());
			} else {
				throw new IllegalArgumentException(
						String.format("Adjustor event of type %s not yet supported", event.getClass().getSimpleName()));
			}
		}
		throw new IllegalArgumentException(String.format("Expected DESEvent of type %s, but got %s",
				AdjustorBasedEvent.class.getSimpleName(), event.getClass().getSimpleName()));
	}

    /**
     * what is this even ????
     *
     * @param usageModel
     * @return
     */
    public UsageModel changeLoad(final UsageModel usageModel) {

            final Workload workload = usageModel.getUsageScenario_UsageModel().get(0).getWorkload_UsageScenario();

            if (workload instanceof OpenWorkload) {
                    final OpenWorkload openload = (OpenWorkload) workload;

            } else if (workload instanceof ClosedWorkload) {

                    final ClosedWorkload closedload = (ClosedWorkload) workload;

                    final PCMRandomVariable var = CoreFactory.eINSTANCE.createPCMRandomVariable();
                    var.setSpecification(String.valueOf(5));

                    closedload.setThinkTime(var.getSpecification());
            }

            return usageModel;
    }

}