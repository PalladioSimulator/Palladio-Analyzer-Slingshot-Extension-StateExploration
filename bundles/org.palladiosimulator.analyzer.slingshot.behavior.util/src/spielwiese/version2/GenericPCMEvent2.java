package spielwiese.version2;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractGenericEvent;

public class GenericPCMEvent2<M> extends AbstractGenericEvent<M, M> {

	
	public GenericPCMEvent2(final M modelElement) {
		super((Class<M>) modelElement.getClass(), modelElement, 0.0);
	}
}
