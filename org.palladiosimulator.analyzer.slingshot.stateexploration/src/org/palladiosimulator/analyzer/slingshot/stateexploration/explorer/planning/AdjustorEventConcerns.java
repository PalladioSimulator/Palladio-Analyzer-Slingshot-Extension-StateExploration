package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 * To be used during exploration planning.
 *
 * Responsible for things concerned with / related to
 * {@link ModelAdjustmentRequested} events.
 *
 * @author Sarah Stieß
 *
 */
public class AdjustorEventConcerns {

	private final ArchitectureConfiguration config;

	private static final Logger LOGGER = Logger.getLogger(AdjustorEventConcerns.class.getName());

	public AdjustorEventConcerns(final ArchitectureConfiguration config) {
		this.config = config;
	}

	/**
	 * Create copy of the given event.
	 *
	 * The copy references a scaling Policy from the current
	 * {@link ArchitectureConfiguration}
	 *
	 * @param event event to be copied
	 * @return copy of event
	 */
	public ModelAdjustmentRequested copy(final ModelAdjustmentRequested event) {
		return new ModelAdjustmentRequested(this.getMatchingPolicy(event.getScalingPolicy()));
	}


	/**
	 * Get a {@link ScalingPolicy} matching the given {@code appliedPolicy} from the new architecture configuration.
	 *
	 * @param appliedPolicy policy to find the copy of.
	 * @return {@link ScalingPolicy} that is a copy of the given policy.
	 *
	 * @throws NoSuchElementException if the new config has no resource container matching the given {@code id}.
	 */
	private ScalingPolicy getMatchingPolicy(final ScalingPolicy appliedPolicy) {

		final Optional<ScalingPolicy> copiedPolicy = config.getSPD().getScalingPolicies().stream().filter(policy -> policy.getId().equals(appliedPolicy.getId())).findAny();
		if (copiedPolicy.isEmpty()) {
			throw new NoSuchElementException(String.format(
					"No Scaling Policy matching ID %s in new Architectur Configuration.", appliedPolicy.getId()));
		}
		return copiedPolicy.get();
	}

}
