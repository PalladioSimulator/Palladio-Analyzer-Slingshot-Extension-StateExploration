package spielwiese.version1;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

public class EventAndType {

	public DESEvent getEvent() {
		return event;
	}

	public String getType() {
		return type;
	}

	private final DESEvent event;
	private final String type;
	
	public EventAndType(final DESEvent event, final String type) {
		super();
		this.event = event;
		this.type = type;
	}	
}
