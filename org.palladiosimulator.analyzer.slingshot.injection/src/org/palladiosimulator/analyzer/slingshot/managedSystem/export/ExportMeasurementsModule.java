package org.palladiosimulator.analyzer.slingshot.managedSystem.export;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.managedSystem.export.messages.MeasurementsExported;
import org.palladiosimulator.analyzer.slingshot.managedSystem.export.messages.MeasurementsRequested;
import org.palladiosimulator.analyzer.slingshot.networking.data.Message;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

/**
 *
 *
 * @author Sarah Stieß
 *
 */
public class ExportMeasurementsModule extends AbstractSlingshotExtension {

    @Override
    protected void configure() {
        install(ExportSystemBehaviour.class);

        final var messageBinder = MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {
        }, new TypeLiteral<Class<? extends Message<?>>>() {
        });

        messageBinder.addBinding(MeasurementsRequested.MESSAGE_MAPPING_IDENTIFIER)
            .toInstance(MeasurementsRequested.class);
        messageBinder.addBinding(MeasurementsExported.MESSAGE_MAPPING_IDENTIFIER)
            .toInstance(MeasurementsExported.class);
    }
}
