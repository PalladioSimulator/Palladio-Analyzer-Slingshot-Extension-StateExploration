package org.palladiosimulator.analyzer.slingshot.planner.data;

public class Transition {
	private transient State source;
	private State target;
	
	private Reason reason;
	
	public Transition(State target, State source, Reason reason) {
		super();
		this.target = target;
		this.source = source;
		this.reason = reason;
	}

	public State getTarget() {
		return target;
	}

	public State getSource() {
		return source;
	}
	
	public Reason getReason() {
		return reason;
	}
}
