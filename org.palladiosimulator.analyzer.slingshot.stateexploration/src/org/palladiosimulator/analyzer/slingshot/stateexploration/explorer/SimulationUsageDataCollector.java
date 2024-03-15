package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import javax.inject.Inject;

import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserEntryRequested;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.networking.ws.GsonProvider;
import org.palladiosimulator.analyzer.slingshot.networking.ws.Message;
import org.palladiosimulator.analyzer.slingshot.networking.ws.SlingshotWebsocketClient;


@OnEvent(when = UserEntryRequested.class)
public class SimulationUsageDataCollector implements SimulationBehaviorExtension {
	@Inject
	private GsonProvider gsonProvider;
	@Inject
	private SlingshotWebsocketClient client;
	
	
	public record EntryData(String userId, String entityName, String signatureName, String signatureId) {}
	
	public class EntryDataMessage extends Message<EntryData> {
		public EntryDataMessage(EntryData payload) {
			super("Entry", payload, "Explorer");
		}
	}
	
	@Subscribe
	public void onUserEntryRequest(UserEntryRequested request) {
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
