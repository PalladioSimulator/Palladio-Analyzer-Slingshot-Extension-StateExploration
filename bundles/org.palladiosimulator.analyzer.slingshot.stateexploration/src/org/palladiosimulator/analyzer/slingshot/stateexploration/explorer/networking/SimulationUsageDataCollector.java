package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.networking;

import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserEntryRequested;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.networking.data.Message;

/**
 *
 * Note: Please do not send messages directly via
 * {@code SlingshotWebsocketClient#send(String)}. Instead post them to the
 * system bus and let {@code EventMessageDispatcher} take care of it.
 *
 * @author
 *
 */
@OnEvent(when = UserEntryRequested.class)
public class SimulationUsageDataCollector implements SimulationBehaviorExtension {

	public record EntryData(String userId, String entityName, String signatureName, String signatureId) {}

	public class EntryDataMessage extends Message<EntryData> {
		public EntryDataMessage(final EntryData payload) {
			super("Entry", payload, "Explorer");
		}
	}

	@Subscribe
	public void onUserEntryRequest(final UserEntryRequested request) {
		// send to system bus first.
		/*
		try {
			client.sendMessage(new EntryDataMessage(new EntryData(
					request.getEntity().getUser().getId(),
					request.getEntity().getOperationProvidedRole().getEntityName(),
					request.getEntity().getOperationSignature().getEntityName(),
					request.getEntity().getOperationSignature().getId()
					)));
		} catch (Throwable ee) {
			ee.printStackTrace();
			System.exit(0);
		}
		 */
	}

}
