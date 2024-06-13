package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.stateexploration.controller.ExplorerControllerSystemBehaviour;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.ExplorationConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.SLOModelConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.providers.SLOModelProvider;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;




public class ExplorationModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {
		install(ExplorationConfiguration.class);
		install(SLOModelConfiguration.class);
		provideModel(ServiceLevelObjectiveRepository.class, SLOModelProvider.class);

		//<<<<<<< HEAD
		//		configureGson();
		//
		//		final var messageBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {}, new TypeLiteral<Class<? extends Message<?>>>() {});
		//
		//		messageBinder.addBinding("Test").toInstance(TestMessage.class);
		//		messageBinder.addBinding("TestSimEvent").toInstance(SimTestMessage.class);
		//
		//		messageBinder.addBinding("StateExplored").toInstance(StateExploredMessage.class);
		//		messageBinder.addBinding("RequestArchitecture").toInstance(RequestArchitectureMessage.class);
		//
		//		install(ExplorationMessageDispatcher.class);
		//		install(SimulationUsageDataCollector.class);
		//		install(SimulationBehaviourReactionTest.class);
		//		install(ArchitectureMessageDispatcher.class);
		//	}
		//
		//	private void configureGson() {
		//		// Configure Gson
		//		final var gsonBinder = MapBinder.newMapBinder(binder(), Type.class, Object.class);
		//
		//		gsonBinder.addBinding(ScalingPolicy.class).toInstance(new JsonSerializer<ScalingPolicy>() {
		//			@Override
		//			public JsonElement serialize(final ScalingPolicy src, final Type typeOfSrc, final JsonSerializationContext context) {
		//				final JsonObject jsonScalingPolicy = new JsonObject();
		//
		//				jsonScalingPolicy.addProperty("id", src.getId());
		//				jsonScalingPolicy.addProperty("name", src.getEntityName());
		//				jsonScalingPolicy.addProperty("uri", src.eResource().getURI().toString());
		//
		//				return jsonScalingPolicy;
		//			}
		//		});
		//=======
		install(ExplorerControllerSystemBehaviour.class);
	}

}
