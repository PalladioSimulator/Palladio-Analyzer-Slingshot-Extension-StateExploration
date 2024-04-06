package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.SystemBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.networking.ws.GsonProvider;
import org.palladiosimulator.analyzer.slingshot.networking.ws.Message;
import org.palladiosimulator.analyzer.slingshot.networking.ws.SlingshotWebsocketClient;
import org.palladiosimulator.analyzer.slingshot.planner.data.ContainerSpecification;
import org.palladiosimulator.analyzer.slingshot.planner.data.HDDContainerSpecification;
import org.palladiosimulator.analyzer.slingshot.planner.data.LinkSpecification;
import org.palladiosimulator.pcm.resourceenvironment.CommunicationLinkResourceSpecification;
import org.palladiosimulator.pcm.resourceenvironment.HDDProcessingResourceSpecification;
import org.palladiosimulator.pcm.resourceenvironment.LinkingResource;
import org.palladiosimulator.pcm.resourceenvironment.ProcessingResourceSpecification;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;

import de.uka.ipd.sdq.stoex.DoubleLiteral;
import de.uka.ipd.sdq.stoex.IntLiteral;

@OnEvent(when = RequestArchitectureMessage.class)
public class ArchitectureMessageDispatcher implements SystemBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(ArchitectureMessageDispatcher.class.getName());
	@Inject
	private GsonProvider gsonProvider;
	@Inject
	private SlingshotWebsocketClient client;

	public enum ResourceType {HHD, PROCESSING}
	public record Link(String id, List<String> connected) {}
	public record ResourceContainer(String id, ResourceType type) {}
	public record Architecture(List<ResourceContainer> resourceContainer, List<Link> links) {}
	
	public class ArchitectureMessage extends Message<Architecture> {
		public ArchitectureMessage(Architecture payload) {
			super("ArchitectureMessage", payload, "Explorer");
		}
	}
	
	@Subscribe
	public void onMessageRecieved(RequestArchitectureMessage sim) {
		try {
			PCMResourceSetPartitionProvider pcmResourceSetPartition = Slingshot.getInstance().getInstance(PCMResourceSetPartitionProvider.class);
			ResourceEnvironment re = pcmResourceSetPartition.get().getResourceEnvironment();
			
			List<Link> links = new ArrayList<Link>();
			List<ResourceContainer> resourceContainer = new ArrayList<ResourceContainer>();
			
			for (LinkingResource x : re.getLinkingResources__ResourceEnvironment()) {
				links.add(new Link(x.getId(), x.getConnectedResourceContainers_LinkingResource().stream().map(y -> y.getId()).toList()));
			}
			for (org.palladiosimulator.pcm.resourceenvironment.ResourceContainer x : re.getResourceContainer_ResourceEnvironment()) {
				for (ProcessingResourceSpecification y : x.getActiveResourceSpecifications_ResourceContainer()) {
					resourceContainer.add(new ResourceContainer(x.getId(), ResourceType.PROCESSING));
				}
				for (HDDProcessingResourceSpecification y : x.getHddResourceSpecifications()) {
					resourceContainer.add(new ResourceContainer(x.getId(), ResourceType.HHD));
				}
			}
			
			client.sendMessage(new ArchitectureMessage(new Architecture(resourceContainer, links)));
		} catch (Throwable ee) {
			ee.printStackTrace();
			System.exit(0);
		}
	}
}
