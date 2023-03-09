package org.palladiosimulator.analyzer.slingshot.snapshot.api;

import java.util.Set;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractEntityChangedEvent;

/**
 *
 * @author stiesssh
 *
 */
public interface EventRecord {
	public void updateRecord(final AbstractEntityChangedEvent<?> event);
	public Set<AbstractEntityChangedEvent<?>> getRecord();
}
