package org.palladiosimulator.analyzer.slingshot.planner.data;

public class Transition {
	private transient StateGraphNode source;
	private StateGraphNode target;
	
	private Reason reason;
	
	public Transition(StateGraphNode target, StateGraphNode source, Reason reason) {
		super();
		this.target = target;
		this.source = source;
		this.reason = reason;
	}

	public StateGraphNode getTarget() {
		return target;
	}

	public void setSource(StateGraphNode s) {
		source = s;
	}
	
	public StateGraphNode getSource() {
		return source;
	}
	
	public Reason getReason() {
		return reason;
	}
}
