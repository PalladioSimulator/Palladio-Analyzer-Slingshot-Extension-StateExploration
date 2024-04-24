package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.resource.Resource;
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
	
	private static class ArchitectureResourceAccessException extends RuntimeException {
		public ArchitectureResourceAccessException(String exception) {
			super(exception);
		}
	}

	@Subscribe
	public void onMessageRecieved(RequestArchitectureMessage sim) {
		try {
			System.out.println("Reacting to RequestArchitectureMessage");
			PCMResourceSetPartitionProvider pcmResourceSetPartition = Slingshot.getInstance().getInstance(PCMResourceSetPartitionProvider.class);
			
		
			var allocationResource = pcmResourceSetPartition.get().getAllocation().eResource();
			var systemResource = pcmResourceSetPartition.get().getSystem().eResource();
			var resourceEnvironmentResource = pcmResourceSetPartition.get().getResourceEnvironment().eResource();
			var repositoryResource = pcmResourceSetPartition.get().getRepositories().stream().findFirst()
					.orElseThrow(() -> new ArchitectureResourceAccessException("Could not access repository")).eResource();
			var spdResource = pcmResourceSetPartition.get().getElement(SpdPackage.eINSTANCE.getSPD()).stream().findFirst()
					.orElseThrow(() -> new ArchitectureResourceAccessException("Could not access repository")).eResource();
			
		
			var resources = List.of(allocationResource, systemResource, resourceEnvironmentResource, repositoryResource, spdResource);
			
			resources.forEach(res -> {
				var path = Paths.get(getAbsolutePath(res));
				try {
					var fileBytes = Files.readAllBytes(path);
					var message = new String(fileBytes, StandardCharsets.UTF_8);
					client.sendMessage(new ArchitectureMessage(message));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new ArchitectureResourceAccessException("Failed to read content of: " + path);
				}
			});
		} catch (Throwable t) {
			t.printStackTrace();
		}

	}
	
	private String getAbsolutePath(Resource resource) {
		var uri = resource.getURI().toPlatformString(false);
		String projectName = uri.split("/")[1];
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        IPath location = project.getLocation();  // This gets the absolute path to the linked resource
        System.out.println("Linked resource absolute path: " + location.toOSString());
        
        var res = Arrays.stream(uri.split("/"))
        	.filter(x -> !("".equals(x) || projectName.equals(x)))
        	.reduce(location.toOSString(), (x,y) -> x+"/"+y);
        
        return res;
	}


}
