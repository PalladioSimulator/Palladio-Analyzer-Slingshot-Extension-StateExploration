package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.networking.SlingshotWebsocketClient;
import org.palladiosimulator.analyzer.slingshot.networking.events.Message;
import org.palladiosimulator.analyzer.slingshot.networking.util.GsonProvider;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.spd.SpdPackage;


@OnEvent(when = RequestArchitectureMessage.class)
public class ArchitectureMessageDispatcher implements SystemBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(ArchitectureMessageDispatcher.class.getName());
	@Inject
	private GsonProvider gsonProvider;
	@Inject
	private SlingshotWebsocketClient client;

	public class ArchitectureMessage extends Message<String> {
		public ArchitectureMessage(String payload) {
			super("ArchitectureMessage", payload, "Explorer");
		}
	}

	@Subscribe
	public void onMessageRecieved(RequestArchitectureMessage sim) {
		try {
			PCMResourceSetPartitionProvider pcmResourceSetPartition = Slingshot.getInstance().getInstance(PCMResourceSetPartitionProvider.class);
			
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			File workspaceDirectory = workspace.getRoot().getLocation().toFile();
			String workspaceDirectoryPath = workspaceDirectory.getAbsolutePath();

			URI allocationURI = pcmResourceSetPartition.get().getAllocation().eResource().getURI();
			URI systemURI = pcmResourceSetPartition.get().getSystem().eResource().getURI();
			URI resourceEnvironmentURI = pcmResourceSetPartition.get().getResourceEnvironment().eResource().getURI();
			URI repositoryURI = null;
			Optional<Repository> reps = pcmResourceSetPartition.get().getRepositories().stream().filter(x -> x.eResource().getURI().toString().endsWith("default.repository")).findFirst();
			if (reps.isPresent()) {
				repositoryURI = reps.get().eResource().getURI();
			}
			URI spdURI = null;
			if (pcmResourceSetPartition.get().getElement(SpdPackage.eINSTANCE.getSPD()).size() > 0) {	
				spdURI = pcmResourceSetPartition.get().getElement(SpdPackage.eINSTANCE.getSPD()).get(0).eResource().getURI();
			}
			

			System.out.println("Resource Files:");
			System.out.println(workspaceDirectoryPath + allocationURI.toPlatformString(false));
			File allocationFile = new File(workspaceDirectoryPath + allocationURI.toPlatformString(false));
			System.out.println(workspaceDirectoryPath + systemURI.toPlatformString(false));
			File systemFile = new File(workspaceDirectoryPath + systemURI.toPlatformString(false));
			System.out.println(workspaceDirectoryPath + resourceEnvironmentURI.toPlatformString(false));
			File resourceEnvironmentFile = new File(workspaceDirectoryPath + resourceEnvironmentURI.toPlatformString(false));
			File repositoryFile = null;
			if (!repositoryURI.toString().equals("")) {
				System.out.println(workspaceDirectoryPath + repositoryURI.toPlatformString(false));
				repositoryFile = new File(workspaceDirectoryPath + repositoryURI.toPlatformString(false));
			}
			File spdFile = null;
			if (spdURI != null) {
				System.out.println(workspaceDirectoryPath + spdURI.toPlatformString(false));
				spdFile = new File(workspaceDirectoryPath + spdURI.toPlatformString(false));
			}

			
			sendFile(allocationFile.getAbsolutePath());
			sendFile(systemFile.getAbsolutePath());
			sendFile(resourceEnvironmentFile.getAbsolutePath());
			if (repositoryFile != null) {
				sendFile(repositoryFile.getAbsolutePath());
			}
			if (spdFile != null) {
				sendFile(spdFile.getAbsolutePath());
			}
			
		} catch (Throwable ee) {
			ee.printStackTrace();
			System.exit(0);
		}
	}

	private void sendFile(String filePath) {
		StringBuilder xmlStringBuilder = new StringBuilder();
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				xmlStringBuilder.append(line);
			}

			JSONObject jsonObject = XML.toJSONObject(xmlStringBuilder.toString());
			client.sendMessage(new ArchitectureMessage(gsonProvider.getGson().toJson(jsonObject)));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
