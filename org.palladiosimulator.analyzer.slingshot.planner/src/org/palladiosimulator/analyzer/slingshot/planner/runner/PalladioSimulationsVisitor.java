package org.palladiosimulator.analyzer.slingshot.planner.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.measure.Measure;
import javax.measure.quantity.Duration;

import org.palladiosimulator.analyzer.slingshot.planner.data.Measurement;
import org.palladiosimulator.analyzer.slingshot.planner.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.planner.data.SLO;
import org.palladiosimulator.edp2.dao.MeasurementsDao;
import org.palladiosimulator.edp2.dao.exception.DataNotAccessibleException;
import org.palladiosimulator.edp2.models.ExperimentData.DataSeries;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentRun;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.ExperimentData.MeasurementRange;
import org.palladiosimulator.edp2.models.ExperimentData.RawMeasurements;
import org.palladiosimulator.edp2.util.MeasurementsUtility;
import org.palladiosimulator.edp2.util.MetricDescriptionUtility;
import org.palladiosimulator.metricspec.BaseMetricDescription;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;

/**
 * Static functions to extract information from different Palladio Objects
 * 
 * Flats the beforehand 4 steps deep nesting to 2.
 * 
 * @author Jonas Edlhuber
 *
 */
public class PalladioSimulationsVisitor {
	public static SLO visitServiceLevelObjective(ServiceLevelObjective slo) {
		return new SLO(slo.getName(), (Number) slo.getLowerThreshold().getThresholdLimit().getValue(), (Number) slo.getUpperThreshold().getThresholdLimit().getValue());
	}

	public static ArrayList<MeasurementSet> visitExperiementSetting(ExperimentSetting es) {
		ArrayList<MeasurementSet> runs = new ArrayList<MeasurementSet>();

		for (ExperimentRun er : es.getExperimentRuns()) {
			runs.addAll(PalladioSimulationsVisitor.visitExperimentRun(er));
		}

		return runs;
	}

	public static ArrayList<MeasurementSet> visitExperimentRun(ExperimentRun er) {
		ArrayList<MeasurementSet> ms = new ArrayList<MeasurementSet>();

		for (org.palladiosimulator.edp2.models.ExperimentData.Measurement m : er.getMeasurement()) {
			ms.addAll(PalladioSimulationsVisitor.visitMeasurment(m));
		}

		return ms;
	}

	public static ArrayList<MeasurementSet> visitMeasurment(org.palladiosimulator.edp2.models.ExperimentData.Measurement m) {
		ArrayList<MeasurementSet> mrs = new ArrayList<MeasurementSet>();

		for (MeasurementRange mr : m.getMeasurementRanges()) {
			mrs.add(PalladioSimulationsVisitor.visitMeasurementRange(mr));
		}

		return mrs;
	}

	public static MeasurementSet visitMeasurementRange(MeasurementRange mr) {
		if (mr.getRawMeasurements() == null)
			throw new IllegalStateException("No RawMeasurments found!");

		return PalladioSimulationsVisitor.visitRawMeasurments(mr.getRawMeasurements());
	}

	/**
	 * This extracts the information of all data series from the raw measurement. It
	 * is asserted that the first data series contains the point in time and second
	 * the measured values.
	 * 
	 * More that one data series with values is ignored at the moment!
	 * 
	 * @param rawMeasurments
	 * @return
	 */
	public static MeasurementSet visitRawMeasurments(RawMeasurements rawMeasurments) {
		MeasurementSet measurments = new MeasurementSet();

		ArrayList<Double> pointInTime = null;
		ArrayList<Double> values = null;

		for (int i = 0; i < rawMeasurments.getDataSeries().size(); i++) {
			DataSeries dataSeries = rawMeasurments.getDataSeries().get(i);

			if (rawMeasurments.getMeasurementRange().getMeasurement().getMeasuringType().getMetric()
					.eContainer() == null)
				throw new IllegalStateException("MetricDescription not the one from common metrics.");

			// it is presumed that the the index of the metric is correlated to the index of
			// the data series
			BaseMetricDescription bmc = MetricDescriptionUtility.toBaseMetricDescriptions(
					rawMeasurments.getMeasurementRange().getMeasurement().getMeasuringType().getMetric())[i];

			// TODO: using the BasicMetricDescription it can be decided which type of Measure<?, ?> will be returened
//			System.out.println(bmc.getName());
//			System.out.println(bmc.getScale().getName());
//			System.out.println(bmc.getDataType().getName());
//			System.out.println(bmc.getCaptureType().getName());	
//			System.out.println(bmc.getScopeOfValidity().getName());
			
			
			if (i == 0 && !bmc.getName().equals("Point in Time")) {
				throw new IllegalStateException("First DataSeries is not Point in Time, execution aboarded!");
			} else if (i == 0) {
				pointInTime = PalladioSimulationsVisitor.visitDataSeries(dataSeries);
			} else if (i == 1) {
				values = PalladioSimulationsVisitor.visitDataSeries(dataSeries);
				measurments.setName(bmc.getName()); // setting description of the values
			}
		}

		if (pointInTime == null || values == null || !(pointInTime.size() == values.size()))
			throw new IllegalStateException("Number of point in time values and measurments value do not match!");

		for (int i = 0; i < pointInTime.size(); i++) {
			measurments.add(new Measurement<Double>(values.get(i), pointInTime.get(i)));
		}

		return measurments;
	}

	/**
	 * Visit a data series and return the double values in an array list.
	 * 
	 * @param ds DataSeries with doubles
	 * @return ArrayList of Doubles
	 */
	private static ArrayList<Double> visitDataSeries(DataSeries ds) {
		MeasurementsDao<Double, Duration> dao = 
				(MeasurementsDao<Double, Duration>) MeasurementsUtility.<Duration>getMeasurementsDao(ds);

		List<Measure<Double, Duration>> measures = dao.getMeasurements();
		
		ArrayList<Double> doubles = new ArrayList<Double>(
				measures.stream().map(measure -> measure.getValue()).collect(Collectors.toList()));

		try {
			dao.close();
		} catch (DataNotAccessibleException e) {
			e.printStackTrace();
		}

		return doubles;
	}

}