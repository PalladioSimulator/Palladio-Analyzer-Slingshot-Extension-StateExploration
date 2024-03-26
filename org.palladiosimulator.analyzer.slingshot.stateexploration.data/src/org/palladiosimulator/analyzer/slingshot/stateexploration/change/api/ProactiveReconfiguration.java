package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;

/**
 *
 * Contains everything related to a reconfiguration transition.
 *
 * This is the *ChangeApplication* from the DomainModel, but only for Reconfiguration (Scaling)
 *
 * According to DomainModel, the *CahngeApplication* is connected to :
 *  - Change => ScalingPolicy.
 *  - Transition => RawTransition owning this Change.
 *  - Binding (to PCM Element) => get via result or policy
 *  - Condition => get from policy, should be the trigger.
 *  - Effect => idk, maybe try to look into the successor states??
 *
 * Oh, I missed something: the policies (usually) evalutate against aggragated
 * values, but in the states, i can only provide the plain values (because all
 * i get are plain values) but in case of reactive Reconfiguration, planning
 * probably wants to know by what value the threshold was crossed. And we are
 * back at the condition form the Domain Model ;)
 *
 * Condition consist of the trigger/threshold (defined in policy) and the actual value.
 * however, the value part is kinda... still missing in the simulator? also triggers
 * got some drastic changes with the last SDP update.
 * Actually, i think it makes sense to include this into the AdjustmentResult.
 * TODO : talk to floriment and julijan
 *
 * @author stiesssh
 *
 */
public class ProactiveReconfiguration extends Reconfiguration {

	public ProactiveReconfiguration(final ModelAdjustmentRequested event) {
		super(event);
	}
}
