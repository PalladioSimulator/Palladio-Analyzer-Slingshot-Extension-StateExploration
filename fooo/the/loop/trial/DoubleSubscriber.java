package close.the.loop.trial;

import javax.inject.Inject;

import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.ui.events.ArchitectureModelsTabBuilderStarted;

import close.the.loop.trial.events.TrialSystemEvent;


@OnEvent(when = SimulationStarted.class, then = {TrialSystemEvent.class})
@OnEvent(when = ArchitectureModelsTabBuilderStarted.class, then = {})
@OnEvent(when = TrialSystemEvent.class, then = {})
public class DoubleSubscriber implements SimulationBehaviorExtension, SystemBehaviorExtension {

	@Inject
	public DoubleSubscriber() {
	}

	@Override
	public boolean isActive() {
		return true;
	}
	
	private final SystemDriver systemDriver = Slingshot.getInstance().getSystemDriver();
	
	
	@Subscribe
	public Result<TrialSystemEvent> onConfigurationStarted(final SimulationStarted event) {
		System.out.println("foo");
		systemDriver.postEvent(new TrialSystemEvent());
		return Result.of();
	}
	
	@Subscribe
	public void onArchitectureModelsTabBuilderStarted(final ArchitectureModelsTabBuilderStarted event) {
		System.out.println("bar");
	}
	
	@Subscribe
	public void onTrialSystemEvent(final TrialSystemEvent event) {
		System.out.println("trial");
	}
}
