package org.palladiosimulator.analyzer.slingshot.planner.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.measure.Measure;
import javax.measure.quantity.Duration;

import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
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
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;

import com.google.common.base.Objects;

import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;


/**
 * Static functions to extract information from different Palladio Objects
 * 
 * Flats the beforehand 4 steps deep nesting to 2.
 * 
 * @author Jonas Edlhuber
 *
 */
public class PalladioSimulationsVisitor {
	public final static String POINT_IN_TIME = "Point in Time";
	public final static String RESPONSE_TIME = "Response Time";
	
	// Workaround to prevent exception while monitors are null 
	public static <T> T safe(Supplier<T> resolver) {
	    try {
	        return resolver.get();
	    } catch (NullPointerException e) {
	        return null;
	    }
	}

	
	public static SLO visitServiceLevelObjective(ServiceLevelObjective slo) {
		return new SLO(slo.getName(), slo.getMeasurementSpecification().getId(), (Number) slo.getLowerThreshold().getThresholdLimit().getValue(), (Number) slo.getUpperThreshold().getThresholdLimit().getValue());
	}

	public static List<MeasurementSet> visitExperiementSetting(ExperimentSetting es) {
		return es.getExperimentRuns().stream().map(x -> PalladioSimulationsVisitor.visitExperimentRun(x)).flatMap(y -> y.stream()).toList();
	}

	public static List<MeasurementSet> visitExperimentRun(ExperimentRun er) {
		return er.getMeasurement().stream().map(x -> PalladioSimulationsVisitor.visitMeasurment(x)).flatMap(y -> y.stream()).toList();
	}

	public static List<MeasurementSet> visitMeasurment(org.palladiosimulator.edp2.models.ExperimentData.Measurement m) {
		System.out.println("mid2: " + m.getId());
		return m.getMeasurementRanges().stream().map(x -> PalladioSimulationsVisitor.visitMeasurementRange(x)).flatMap(y -> y.stream()).toList();
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

		if (rawMeasurments.getMeasurementRange().getMeasurement().getMeasuringType().getMetric()
				.eContainer() == null)
			throw new IllegalStateException("MetricDescription not the one from common metrics.");

		for (int i = 0; i < rawMeasurments.getDataSeries().size(); i=i+2) {
			
			DataSeries dataSeries1 = rawMeasurments.getDataSeries().get(i);
			DataSeries dataSeries2 = rawMeasurments.getDataSeries().get(i+1);
			
			
			// it is presumed that the the index of the metric is correlated to the index of
			// the data series
			BaseMetricDescription bmc1 = MetricDescriptionUtility.toBaseMetricDescriptions(
					rawMeasurments.getMeasurementRange().getMeasurement().getMeasuringType().getMetric())[i];
			BaseMetricDescription bmc2 = MetricDescriptionUtility.toBaseMetricDescriptions(
					rawMeasurments.getMeasurementRange().getMeasurement().getMeasuringType().getMetric())[i+1];
			
			MeasurementSet ms = processDataSeries(dataSeries1, dataSeries2, bmc1, bmc2);
			
			
			if (ms != null)
				measurments.add(ms);
		}

		return measurments;
	}
	
	
	private static MeasurementSet processDataSeries(DataSeries dataSeries1, DataSeries dataSeries2, BaseMetricDescription bmc1, BaseMetricDescription bmc2) {
		List<Number> pointInTime = null;
		List<Number> values = null;
		String measureName = null;
		String measureURI = null;
		
		if (bmc1.getName().equals(POINT_IN_TIME))
			pointInTime = PalladioSimulationsVisitor.visitDataSeries(dataSeries1);
		else if (bmc2.getName().equals(POINT_IN_TIME))
			pointInTime = PalladioSimulationsVisitor.visitDataSeries(dataSeries2);
		
		MeasuringPoint mpoint = null;
		MetricDescription mc = null;
		
		if (!(bmc1.getName().equals(POINT_IN_TIME))) {
			mc = dataSeries1.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMetric();
			mpoint = dataSeries1.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMeasuringPoint();
			values = PalladioSimulationsVisitor.visitDataSeries(dataSeries1);
			measureName = dataSeries1.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMeasuringPoint().getStringRepresentation();
			measureURI = dataSeries1.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMeasuringPoint().getResourceURIRepresentation();
		} else if (!(bmc2.getName().equals(POINT_IN_TIME))) {
			mc = dataSeries2.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMetric();
			mpoint = dataSeries2.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMeasuringPoint();
			values = PalladioSimulationsVisitor.visitDataSeries(dataSeries2);
			measureName = dataSeries2.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMeasuringPoint().getStringRepresentation();
			measureURI = dataSeries2.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMeasuringPoint().getResourceURIRepresentation();
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
					throw new IllegalStateException("Point in time values do not provide an ascending row!");
			}
			
			var monitorRepo = Slingshot.getInstance().getInstance(MonitorRepository.class);
			
			final String mpointUri = mpoint.getResourceURIRepresentation();
			
			var monitor = monitorRepo.getMonitors().stream().filter(x -> 
				Objects.equal(x.getMeasuringPoint().getResourceURIRepresentation(), mpointUri))
					.findAny().orElse(null);
			
			var specification = monitor.getMeasurementSpecifications().stream()
					.filter(x -> MetricDescriptionUtility.isBaseMetricDescriptionSubsumedByMetricDescription(bmc1, x.getMetricDescription()) ||
							MetricDescriptionUtility.isBaseMetricDescriptionSubsumedByMetricDescription(bmc2, x.getMetricDescription()))
					.findAny().orElse(null);
					
			
			
			MeasurementSet ms = new MeasurementSet();
			var md = specification.getMetricDescription();
			if(specification != null) {
				ms.setSpecificationId(specification.getId());
				ms.setSpecificationName(specification.getName());
				ms.setMonitorId(monitor.getId());
				ms.setMonitorName(monitor.getEntityName());
				ms.setName(measureName);
				ms.setMetricName(md.getName());
				ms.setMetricDescription(md.getTextualDescription());
			}
			
			for (int j = 0; j < pointInTime.size(); j++) {
				ms.add(new Measurement<Number>(values.get(j), (double) pointInTime.get(j)));
			}

			return ms;
		}
		
		
		return null;
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
