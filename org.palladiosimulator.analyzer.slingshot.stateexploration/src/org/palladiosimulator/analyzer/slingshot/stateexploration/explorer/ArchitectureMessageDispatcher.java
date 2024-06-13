package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.resource.Resource;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.networking.SlingshotWebsocketClient;
import org.palladiosimulator.analyzer.slingshot.networking.data.Message;
import org.palladiosimulator.analyzer.slingshot.networking.util.GsonProvider;
import org.palladiosimulator.spd.SpdPackage;


@OnEvent(when = RequestArchitectureMessage.class)
public class ArchitectureMessageDispatcher implements SystemBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(ArchitectureMessageDispatcher.class.getName());
	@Inject
	private GsonProvider gsonProvider;
	@Inject
	private SlingshotWebsocketClient client;

	public class ArchitectureMessage extends Message<String> {
		public ArchitectureMessage(final String payload) {
			super("ArchitectureMessage", payload, "Explorer");
		}
	}

	private static class ArchitectureResourceAccessException extends RuntimeException {
		public ArchitectureResourceAccessException(final String exception) {
			super(exception);
		}
	}

	@Subscribe
	public void onMessageRecieved(final RequestArchitectureMessage sim) {
		try {
			System.out.println("Reacting to RequestArchitectureMessage");
			final PCMResourceSetPartitionProvider pcmResourceSetPartition = Slingshot.getInstance().getInstance(PCMResourceSetPartitionProvider.class);


			final var allocationResource = pcmResourceSetPartition.get().getAllocation().eResource();
			final var systemResource = pcmResourceSetPartition.get().getSystem().eResource();
			final var resourceEnvironmentResource = pcmResourceSetPartition.get().getResourceEnvironment().eResource();
			final var repositoryResource = pcmResourceSetPartition.get().getRepositories().stream().findFirst()
					.orElseThrow(() -> new ArchitectureResourceAccessException("Could not access repository")).eResource();
			final var spdResource = pcmResourceSetPartition.get().getElement(SpdPackage.eINSTANCE.getSPD()).stream().findFirst()
					.orElseThrow(() -> new ArchitectureResourceAccessException("Could not access repository")).eResource();


			final var resources = List.of(allocationResource, systemResource, resourceEnvironmentResource, repositoryResource, spdResource);

			resources.forEach(res -> {
				final var path = Paths.get(getAbsolutePath(res));
				try {
					final var fileBytes = Files.readAllBytes(path);
					final var message = new String(fileBytes, StandardCharsets.UTF_8);
					client.sendMessage(new ArchitectureMessage(message));
				} catch (final IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new ArchitectureResourceAccessException("Failed to read content of: " + path);
				}
			});
		} catch (final Throwable t) {
			t.printStackTrace();
		}

	}

	private String getAbsolutePath(final Resource resource) {
		final var uri = resource.getURI().toPlatformString(false);
		final String projectName = uri.split("/")[1];
		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		final IPath location = project.getLocation();  // This gets the absolute path to the linked resource
		System.out.println("Linked resource absolute path: " + location.toOSString());

		final var res = Arrays.stream(uri.split("/"))
				.filter(x -> !("".equals(x) || projectName.equals(x)))
				.reduce(location.toOSString(), (x,y) -> x+"/"+y);

		return res;
	}


}
