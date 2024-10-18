package org.palladiosimulator.analyzer.slingshot.injection.utility.converter.data;

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

	public void setTotalUtilty(final double totalUtilty) {
		this.totalUtility = totalUtilty;
	}

	public List<UtilityData> getData() {
		return data;
	}

	public void setData(final List<UtilityData> data) {
		this.data = data;
	}

	public void addDataInstance(final String id, final double d, final UtilityType type) {
		data.add(new UtilityData(id, d, type));
	}

	public void calculateTotalUtility() {
		this.totalUtility = data.stream().mapToDouble(x -> x.utility()).sum();
	}


    public record UtilityData(String id, double utility, UtilityType type) {
    }

    public enum UtilityType {
		SLO, COST;
	}
}
