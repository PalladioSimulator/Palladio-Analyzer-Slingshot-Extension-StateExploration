package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.resource.Resource;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.networking.data.EventMessage;
import org.palladiosimulator.analyzer.slingshot.stateexploration.api.ArchitectureConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.messages.RequestArchitectureMessage;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ExploredState;


/**
 *
 * Dispatches one {@link ArchitectureMessage} per model resource.
 *
 *
 * @author Raphael Straub, Sarah Stieß
 *
 */
@OnEvent(when = RequestArchitectureMessage.class)
public class ArchitectureMessageDispatcher implements SystemBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(ArchitectureMessageDispatcher.class.getName());


	public class ArchitectureMessage extends EventMessage<String> {
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
			LOGGER.info("Reacting to RequestArchitectureMessage");
			final ExploredState state = Slingshot.getInstance().getInstance(ExploredState.class);
			if (state == null) {
				LOGGER.info(
						"Cannot post Architecture, because current state is null. Did you already start the exploration?");
				return;
			}
			final ArchitectureConfiguration access = state.getArchitecureConfiguration();

			final var allocationResource = access.getAllocation().eResource();
			final var systemResource = access.getSystem().eResource();
			final var resourceEnvironmentResource = access.getResourceEnvironment().eResource();
			final var repositoryResource = access.getRepository().eResource();

			final var spdResource = access.getSPD()
					.orElseThrow(() -> new ArchitectureResourceAccessException("Could not access spd")).eResource();

			final var resources = List.of(allocationResource, systemResource, resourceEnvironmentResource, repositoryResource, spdResource);

			resources.forEach(res -> {
				final var path = Paths.get(getAbsolutePath(res));
				try {
					final var fileBytes = Files.readAllBytes(path);
					final var message = new String(fileBytes, StandardCharsets.UTF_8);

					Slingshot.getInstance().getSystemDriver().postEvent(new ArchitectureMessage(message));

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

	/**
	 *
	 * Get absolute path from {@code resource}.
	 *
	 * The URI of {@code resource} must either be a platform or a file URI. The
	 * former is usually used in normal simulation runs, the latter in usually used
	 * in headless simualtion runs.
	 *
	 * @param resource
	 * @return absolute path to resource.
	 */
	private String getAbsolutePath(final Resource resource) {
		if (!(resource.getURI().isPlatform() || resource.getURI().isFile())) {
			throw new IllegalArgumentException(String.format("Resource must have platform or file URI, but uri is %s",
					resource.getURI().toString()));
		}
		if (resource.getURI().isPlatform()) {

			final var uri = resource.getURI().toPlatformString(false);
			// this breaks with headless run where URI already is a file URI:
			// file:/[...]/05a21509-7995-4c51-9928-a67bdc28f4b8/default.allocation

			final String projectName = uri.split("/")[1];
			final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			final IPath location = project.getLocation();  // This gets the absolute path to the linked resource
			System.out.println("Linked resource absolute path: " + location.toOSString());

			final var res = Arrays.stream(uri.split("/"))
					.filter(x -> !("".equals(x) || projectName.equals(x)))
					.reduce(location.toOSString(), (x,y) -> x+"/"+y);

			return res;
		} else {
			return resource.getURI().toFileString();
		}
	}


}
