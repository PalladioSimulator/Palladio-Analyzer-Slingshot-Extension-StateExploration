package close.the.loop.trial;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;

public class CloseTheLoopModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {
		install(DoubleSubscriber.class);
	}

}
