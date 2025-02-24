package org.palladiosimulator.analyzer.slingshot.managedsystem.data;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class Utility {

	@Override
	public String toString() {
		return "Utility [totalUtility=" + totalUtility + ", data=" + data + "]";
	}

	private static final Logger LOGGER = Logger.getLogger(Utility.class);

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
		final var slo_util = data.stream().filter(x -> UtilityType.SLO.equals(x.type())).mapToDouble(x -> x.utility())
				.sum();
		final var costs = data.stream().filter(x -> UtilityType.COST.equals(x.type())).mapToDouble(x -> x.utility())
				.sum();

		this.totalUtility = slo_util / -costs;
		if (Double.isNaN(this.totalUtility)) {
			this.totalUtility = 0;
		} else if (Double.isInfinite(this.totalUtility)) {
			this.totalUtility = Double.MAX_VALUE * Math.signum(this.totalUtility);
		}
	}

	record UtilityData(String id, double utility, UtilityType type) {
	}

	enum UtilityType {
		SLO, COST;
	}
}