package org.palladiosimulator.analyzer.slingshot.behavior.util.visitors;

import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelper;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

public class SetReferencingVisitor {

	protected final CloneHelper helper;

	public SetReferencingVisitor(final PCMResourceSetPartition set) {
		this.helper = new CloneHelper(set);
	}
}
