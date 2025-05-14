package spielwiese.version2;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;
import org.palladiosimulator.pcm.usagemodel.UsageModel;

public class GenericPCMEvent extends AbstractEntityChangedEvent<UsageModel> {

	private final UsageModel model;
	
	public GenericPCMEvent(final UsageModel model) {
		super(model, 0.0);
		this.model = model;
	}
}
