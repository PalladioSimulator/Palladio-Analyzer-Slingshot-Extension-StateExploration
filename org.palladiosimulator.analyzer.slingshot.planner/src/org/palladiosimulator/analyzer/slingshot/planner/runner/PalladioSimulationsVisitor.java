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
		return new SLO(slo.getName(), slo.getMeasurementSpecification().getMonitor().getMeasuringPoint().getResourceURIRepresentation(), (Number) slo.getLowerThreshold().getThresholdLimit().getValue(), (Number) slo.getUpperThreshold().getThresholdLimit().getValue());
	}

	public static List<MeasurementSet> visitExperiementSetting(ExperimentSetting es) {
		List<MeasurementSet> runs = new ArrayList<MeasurementSet>();

		for (ExperimentRun er : es.getExperimentRuns()) {
			runs.addAll(PalladioSimulationsVisitor.visitExperimentRun(er));
		}

		return runs;
	}

	public static List<MeasurementSet> visitExperimentRun(ExperimentRun er) {
		List<MeasurementSet> ms = new ArrayList<MeasurementSet>();

		for (org.palladiosimulator.edp2.models.ExperimentData.Measurement m : er.getMeasurement()) {
			ms.addAll(PalladioSimulationsVisitor.visitMeasurment(m));
		}

		return ms;
	}

	public static List<MeasurementSet> visitMeasurment(org.palladiosimulator.edp2.models.ExperimentData.Measurement m) {
		List<MeasurementSet> mrs = new ArrayList<MeasurementSet>();

		for (MeasurementRange mr : m.getMeasurementRanges()) {
			mrs.addAll(PalladioSimulationsVisitor.visitMeasurementRange(mr));
		}

		return mrs;
	}

	public static List<MeasurementSet> visitMeasurementRange(MeasurementRange mr) {
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
	public static List<MeasurementSet> visitRawMeasurments(RawMeasurements rawMeasurments) {
		List<MeasurementSet> measurments = new ArrayList<MeasurementSet>();

		List<Number> pointInTime = null;
		List<Number> values = null;
		String measureName = null;
		String measureURI = null;

		for (int i = 0; i < rawMeasurments.getDataSeries().size(); i++) {
			DataSeries dataSeries = rawMeasurments.getDataSeries().get(i);

			if (rawMeasurments.getMeasurementRange().getMeasurement().getMeasuringType().getMetric()
					.eContainer() == null)
				throw new IllegalStateException("MetricDescription not the one from common metrics.");
			
			// it is presumed that the the index of the metric is correlated to the index of
			// the data series
			BaseMetricDescription bmc = MetricDescriptionUtility.toBaseMetricDescriptions(
					rawMeasurments.getMeasurementRange().getMeasurement().getMeasuringType().getMetric())[i];
			
			if (bmc.getName().equals("Point in Time")) {
				pointInTime = PalladioSimulationsVisitor.visitDataSeries(dataSeries);
			} else {
				values = PalladioSimulationsVisitor.visitDataSeries(dataSeries);
				measureName = bmc.getName();
				measureURI = dataSeries.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMeasuringPoint().getResourceURIRepresentation();
			}
			
			// i found two measures, one with point in time and one with something else
			if (pointInTime != null && values != null) {
				if (!(pointInTime.size() == values.size()))
					throw new IllegalStateException("Number of point in time values and measurments value do not match!");

				// checking if the point in time values do ascend
				Number previous = 0;
				for (Number n : pointInTime) {
					if (n.doubleValue() >= previous.doubleValue())
						previous = n;
					else
						throw new IllegalStateException("Point in time values do not present an ascending row!");
				}
				
				MeasurementSet ms = new MeasurementSet();
				ms.setName(measureName);
				ms.setMeasuringPointURI(measureURI);
				
				for (int j = 0; j < pointInTime.size(); j++) {
					ms.add(new Measurement<Number>(values.get(j), (double) pointInTime.get(j)));
				}

				if (ms.getName().equals("Response Time")) // temporary hiding all but the relevant measure
					measurments.add(ms);
				
				// reset for the next rows
				pointInTime = null;
				values = null;
				measureName = null;
			}
		}

		return measurments;
	}

	/**
	 * Visit a data series and return the double values in an array list.
	 * 
	 * @param ds DataSeries with doubles
	 * @return ArrayList of Doubles
	 */
	private static List<Number> visitDataSeries(DataSeries ds) {
		MeasurementsDao<Number, Duration> dao = 
				(MeasurementsDao<Number, Duration>) MeasurementsUtility.<Duration>getMeasurementsDao(ds);

		List<Measure<Number, Duration>> measures = dao.getMeasurements();
		
		List<Number> numbers = new ArrayList<Number>(
				measures.stream().map(measure -> measure.getValue()).collect(Collectors.toList()));

		try {
			dao.close();
		} catch (DataNotAccessibleException e) {
			e.printStackTrace();
		}

		return numbers;
	}

}