package org.palladiosimulator.analyzer.slingshot.managedSystem;

import javax.inject.Named;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.networking.data.NetworkingConstants;

import com.google.inject.Provides;

/**
 *
 * @author Sarah Stieß
 *
 */
public class ManagedSystemModule extends AbstractSlingshotExtension {

    @Provides
    @Named(NetworkingConstants.CLIENT_NAME)
    public String clientName() {
        return "ManagedSystem";
    }

    @Override
    protected void configure() {
        install(SlowdownBehaviour.class);
    }
}

