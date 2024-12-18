package org.palladiosimulator.analyzer.slingshot.converter.data;

import java.util.LinkedList;
import java.util.List;

public class Utility {
	private double totalUtility;
	private List<UtilityData> data;

	
	public Utility() {
		super();
		this.totalUtility = 0;
		this.data = new LinkedList<>();
	}

	public double getTotalUtilty() {
		return totalUtility;
	}

	public void setTotalUtilty(double totalUtilty) {
		this.totalUtility = totalUtilty;
	}

	public List<UtilityData> getData() {
		return data;
	}

	public void setData(List<UtilityData> data) {
		this.data = data;
	}
	
	public void addDataInstance(String id, double d, UtilityType type) {
		data.add(new UtilityData(id, d, type));
	}
	
	public void calculateTotalUtility() {
		this.totalUtility = data.stream().mapToDouble(x -> x.utility()).sum();
	}
	
	
	record UtilityData(String id, double utility, UtilityType type) {}
	
	enum UtilityType {
		SLO, COST;
	}
}
