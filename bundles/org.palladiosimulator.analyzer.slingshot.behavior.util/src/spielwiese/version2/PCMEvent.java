package spielwiese.version2;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;
import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;
import org.palladiosimulator.pcm.usagemodel.UsageModel;

public class PCMEvent extends AbstractSimulationEvent {

	private final UsageModel model;
	private final AbstractUserAction element;
	private final Thing thing;
	
	public PCMEvent(final UsageModel model, final Thing thing) {
		super();
		this.model = model;
		this.element = model.getUsageScenario_UsageModel().get(0).getScenarioBehaviour_UsageScenario().getActions_ScenarioBehaviour().get(0);
		
		this.thing = thing;
	}
}
