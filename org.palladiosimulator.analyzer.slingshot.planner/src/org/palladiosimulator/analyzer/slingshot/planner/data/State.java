package org.palladiosimulator.analyzer.slingshot.planner.data;

import java.util.ArrayList;
import java.util.List;

public class State {
	private String id;
	private ArrayList<Transition> outTransitions;
	
	private double startTime;
	private double endTime;

	private ArrayList<MeasurementSet> measurements;
	private ArrayList<SLO> slos;
	
	private transient Double utility;
	
	public State(String id, ArrayList<Transition> outTransitions, double startTime, double endTime, ArrayList<MeasurementSet> measurements, ArrayList<SLO> slos) {
		super();
		this.id = id;
		this.outTransitions = outTransitions;
		this.startTime = startTime;
		this.endTime = endTime;
		this.measurements = measurements;
		this.slos = slos;
	}
	
	public State(String id) {
		super();
		this.id = id;
		this.outTransitions = new ArrayList<Transition>();
		this.measurements = new ArrayList<MeasurementSet>();
		this.slos = new ArrayList<SLO>();
	}

	public ArrayList<Transition> getOutTransitions() {
		return outTransitions;
	}
	
	public void addOutTransition(Transition t) {
		outTransitions.add(t);
	}
	
	public String getId() {
		return id;
	}
	
	public ArrayList<SLO> getSLOs() {
		return slos;
	}
	
	public void setSLOs(ArrayList<SLO> slos) {
		this.utility = null;
		this.slos = slos;
	}
	
	/**
	 * This function calculates the utility of the state.
	 * In the form of "(slo1 - measure1) + (slo2 - measure2)"
	 * In addition to this, the sum is multiplied with the duration of the state.
	 * This balances shorter against longer paths so they are compatible.
	 * @return
	 */
	public double getUtiltity() {
		if (utility == null) {
			double value = 0;
			
			for (SLO slo : this.slos) {
				MeasurementSet ms = measurements.stream().filter(x -> x.getMeasuringPointURI().equals(slo.getMeasuringPointURI())).findFirst().orElse(null);
				if (ms != null)
					value += (slo.getUpperThreshold().doubleValue() - ms.getMedian());
			}
			
			utility = this.getDuration() * value;
		}

		return utility;
	}

	public double getStartTime() {
		return startTime;
	}
	
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getEndTime() {
		return endTime;
	}
	
	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public double getDuration() {
		return endTime - startTime;
	}

	public void setMeasurements(ArrayList<MeasurementSet> measurements) {
		this.measurements = measurements;
	}
	
	public ArrayList<MeasurementSet> getMeasurements() {
		return measurements;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		State other = (State) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}

