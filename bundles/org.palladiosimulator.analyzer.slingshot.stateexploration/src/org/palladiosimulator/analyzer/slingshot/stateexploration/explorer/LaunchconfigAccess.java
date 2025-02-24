package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.ui.ExplorationConfiguration;

public class LaunchconfigAccess {
	
	/**
	 * Get {@link ExplorationConfiguration#MAX_EXPLORATION_CYCLES} from launch
	 * configuration parameters map.
	 *
	 * @return number of max exploration cycles
	 */
	public static int getMaxIterations(final Map<String, Object> launchConfigurationParams) {
		final String maxIteration = (String) launchConfigurationParams
				.get(ExplorationConfiguration.MAX_EXPLORATION_CYCLES);

		return Integer.valueOf(maxIteration);
	}
	
	/**
	 * Get {@link ExplorationConfiguration#HORIZON} from launch
	 * configuration parameters map.
	 *
	 * @return length of horizon in seconds
	 */
	public static int getHorizon(final Map<String, Object> launchConfigurationParams) {
		final String horizon = (String) launchConfigurationParams
				.get(ExplorationConfiguration.HORIZON);

		return Integer.valueOf(horizon);
	}

	/**
	 * Get {@link ExplorationConfiguration#MIN_STATE_DURATION} from launch
	 * configuration parameters map.
	 *
	 * @return minimum duration of an exploration cycles
	 */
	public static double getMinDuration(final Map<String, Object> launchConfigurationParams) {
		final String minDuration = (String) launchConfigurationParams
				.get(ExplorationConfiguration.MIN_STATE_DURATION);

		return Double.valueOf(minDuration);
	}

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
				.get(ExplorationConfiguration.SENSIBILITY);

		return Double.valueOf(minDuration);
	}
	
	/**
	 *
	 * Get {@link ExplorationConfiguration#COST_INTERVAL} from launch configuration
	 * parameters map.
	 *
	 * @return interval for measuring costs.
	 */
	public static double getCostInterval(final Map<String, Object> launchConfigurationParams) {
		final String costInterval = (String) launchConfigurationParams
				.get(ExplorationConfiguration.COST_INTERVAL);

		return Double.valueOf(costInterval);
	}
	
	/**
	 *
	 * Get {@link ExplorationConfiguration#COST_AMOUNT} from launch configuration
	 * parameters map.
	 *
	 * @return amount of costs per interval per container.
	 */
	public static double getCostAmount(final Map<String, Object> launchConfigurationParams) {
		final String costAmount = (String) launchConfigurationParams
				.get(ExplorationConfiguration.COST_AMOUNT);

		return Double.valueOf(costAmount);
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
				.get(ExplorationConfiguration.MODEL_LOCATION);

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
