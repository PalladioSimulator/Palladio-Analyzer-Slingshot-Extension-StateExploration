package org.palladiosimulator.analyzer.slingshot.injection;

import javax.inject.Inject;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.injection.data.ApplyPolicyEventMessage;
import org.palladiosimulator.analyzer.slingshot.injection.data.Link;
import org.palladiosimulator.analyzer.slingshot.injection.data.PlanUpdated;

/**
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when=ApplyPolicyEventMessage.class, then= {})
public class InjectionSystemBehaviour implements SystemBehaviorExtension {


	private final Link linkToSimulation;

	@Inject
	public InjectionSystemBehaviour(final Link link,final SystemDriver driver) {
		this.linkToSimulation = link;
		this.linkToSimulation.setSystem(driver);
	}


	@Subscribe
	public Result<ModelAdjustmentRequested> onApplyPolicyEventMessage(final ApplyPolicyEventMessage event) {



		// Fix timing

        final PlanUpdated request = new PlanUpdated(event.getPayload());

        if (event.getEvent() != "fooo") {
            linkToSimulation.postToSimulation(request);
        }

		return Result.of();
	}
}
