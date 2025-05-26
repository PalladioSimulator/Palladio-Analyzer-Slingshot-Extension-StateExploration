package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.Map;

import org.eclipse.emf.common.util.URI;

public class LaunchconfigAccess {
	
	// formerly defined in ExplorationConfiguration
	public static final String SENSIBILITY = "Sensitivity [0, 1 (most sensitive)]";
	public static final String MODEL_LOCATION = "Location for Arch. Configruations";
	
	/**
	 *
	 *
	 * Get {@link ExplorationConfiguration#SENSIBILITY} from launch configuration
	 * parameters map.
	 *
	 * @return sensibility for stopping regarding SLOs.
	 */
	public static double getSensibility(final Map<String, Object> launchConfigurationParams) {
		final String minDuration = (String) launchConfigurationParams
				.get(SENSIBILITY);

		return Double.valueOf(minDuration);
	}

	/**
	 *
	 * Get {@link ExplorationConfiguration#MODEL_LOCATION} from launch configuration
	 * parameters map, if given.
	 *
	 * @return model location URI, as defined in the run config, or the default location if none was defined.
	 */
	public static URI getModelLocation(final Map<String, Object> launchConfigurationParams) {
		final String modelLocation = (String) launchConfigurationParams
				.get(MODEL_LOCATION);

		if (modelLocation.isBlank() || modelLocation.equals("null")) {
			return URI.createFileURI(java.lang.System.getProperty("java.io.tmpdir"));
		}

		final URI uri = URI.createURI(modelLocation);

		if (uri.isPlatform() || uri.isFile()) {
			return uri;
		} else {
			return URI.createFileURI(modelLocation);
		}
	}

}
