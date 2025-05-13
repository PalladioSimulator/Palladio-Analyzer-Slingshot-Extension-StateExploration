package org.palladiosimulator.analyzer.slingshot.snapshot.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorStateValues;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.LessInvasiveInMemoryRecord;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 * This is the camera for taking the snapshot.
 *
 * (my mental image is like this : through my camera's viewfinder i watch stuff
 * change and sometime i release the shutter and get a picture of how stuff
 * looks at a certain point in time.)
 *
 *
 * @author Sophie Stieß
 *
 */
public abstract class Camera {
	
	/** Beware: keep in sync with original */
	protected static final String FAKE = "fakeID";

	/** Access to past events, that must go into the snapshot.*/
	protected final LessInvasiveInMemoryRecord record;

	/** Access to future events, that must go into the snapshot.*/
	protected final SimulationEngine engine;

	/** Required argument for creating clone helpers*/
	protected final PCMResourceSetPartition set;

	protected final List<DESEvent> additionalEvents = new ArrayList<>();
	protected final Collection<SPDAdjustorStateValues> policyIdToValues;

	public Camera(final LessInvasiveInMemoryRecord record, final SimulationEngine engine,
			final PCMResourceSetPartition set, final Collection<SPDAdjustorStateValues> policyIdToValues) {
		this.record = record;
		this.engine = engine;

		this.policyIdToValues = policyIdToValues;
		this.set = set;
	}
	
	/**
	 * ..and this is like releasing the shutter.
	 */
	public abstract Snapshot takeSnapshot();
	
	/**
	 * include some more events.
	 */
	public void addEvent(final DESEvent event) {
		additionalEvents.add(event);
	}
	
	/**
	 * 
	 * @return
	 */
	protected List<SPDAdjustorStateValues> snapStateValues(){
		return this.policyIdToValues.stream().map(s -> this.copyAndOffset(s, engine.getSimulationInformation().currentSimulationTime())).toList();
	}
	
	/**
	 * Adjust the time of the latest adjustment and the time of the cooldown to the
	 * reference time. Also creates a copy.
	 *
	 * If the latest adjustment was at t = 5 s, the cooldown ends at t = 15 s, and
	 * the reference time is t = 10 s, then the adjusted values will be latest
	 * adjustment at t = -5 s and cooldown end at t = 5 s.
	 *
	 * @param stateValues   values to be adjusted
	 * @param referenceTime time to adjust to.
	 * @return copy of state values with adjusted values.
	 */
	private SPDAdjustorStateValues copyAndOffset(final SPDAdjustorStateValues stateValues, final double referenceTime) {
		final double latestAdjustmentAtSimulationTime = stateValues.latestAdjustmentAtSimulationTime() - referenceTime;
		final int numberScales = stateValues.numberScales();
		final double coolDownEnd = stateValues.coolDownEnd() > 0.0 ? stateValues.coolDownEnd() - referenceTime : 0.0;
		final int numberOfScalesInCooldown = stateValues.numberOfScalesInCooldown();

		final List<ScalingPolicy> enactedPolicies = new ArrayList<>(stateValues.enactedPolicies()); // unchanged
		final List<Double> enactmentTimeOfPolicies = stateValues.enactmentTimeOfPolicies().stream()
				.map(time -> time - referenceTime).toList();

		return new SPDAdjustorStateValues(stateValues.scalingPolicy(), latestAdjustmentAtSimulationTime, numberScales,
				coolDownEnd, numberOfScalesInCooldown, enactedPolicies, enactmentTimeOfPolicies);
	}
}
