package spielwiese.version2;

import java.util.Optional;
import java.util.UUID;

import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;

public class PCMThing extends Thing {
	
	private final UsageScenario element;
	private final Optional<Thing> optionalThing;
	
	public PCMThing(final UsageModel model, final Thing thing) {
		super(UUID.randomUUID().toString(), thing);
		this.element = model.getUsageScenario_UsageModel().get(0);

		
		this.optionalThing = thing.getAnotherThing() == null ? Optional.empty() : Optional.of(thing.getAnotherThing());
	}

}
