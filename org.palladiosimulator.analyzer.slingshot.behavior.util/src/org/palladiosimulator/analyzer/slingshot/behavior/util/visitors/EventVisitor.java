package org.palladiosimulator.analyzer.slingshot.behavior.util.visitors;

import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;

public interface EventVisitor {
	public DESEvent visit(DESEvent e);
}
