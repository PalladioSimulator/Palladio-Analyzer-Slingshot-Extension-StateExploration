package org.palladiosimulator.analyzer.slingshot.planner.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


import javax.measure.quantity.Duration;

import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.planner.data.MeasurementSet;
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
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import com.google.common.base.Objects;



/**
 * Static functions to extract information from different Palladio Objects
 * 
 * Flats the beforehand 4 steps deep nesting to 2.
 * 
 * @author Jonas Edlhuber
 *
 */
public class MeasurementConverter {
	public final static String POINT_IN_TIME = "Point in Time";
	public final static String RESPONSE_TIME = "Response Time";
	


	public static List<MeasurementSet> visitExperiementSetting(ExperimentSetting es) {
		return es.getExperimentRuns().stream().map(x -> MeasurementConverter.visitExperimentRun(x)).flatMap(y -> y.stream()).toList();
	}

	public static List<MeasurementSet> visitExperimentRun(ExperimentRun er) {
		return er.getMeasurement().stream().map(x -> MeasurementConverter.visitMeasurment(x)).flatMap(y -> y.stream()).toList();
	}

	public static List<MeasurementSet> visitMeasurment(org.palladiosimulator.edp2.models.ExperimentData.Measurement m) {
		return m.getMeasurementRanges().stream().map(x -> MeasurementConverter.visitMeasurementRange(x)).flatMap(y -> y.stream()).toList();
	}

	public static List<MeasurementSet> visitMeasurementRange(MeasurementRange mr) {
		if (mr.getRawMeasurements() == null)
			throw new IllegalStateException("No RawMeasurments found!");
		
		return MeasurementConverter.visitRawMeasurments(mr.getRawMeasurements());
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

		if (rawMeasurments.getMeasurementRange().getMeasurement().getMeasuringType().getMetric().eContainer() == null)
			throw new IllegalStateException("MetricDescription not the one from common metrics.");

		for (int i = 0; i < rawMeasurments.getDataSeries().size(); i=i+2) {
			
			var dataSeries1 = rawMeasurments.getDataSeries().get(i);
			var dataSeries2 = rawMeasurments.getDataSeries().get(i+1);
			
			
			// it is presumed that the the index of the metric is correlated to the index of the data series
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
		Predicate<? super MeasurementSpecification> filter = (x -> 
			MetricDescriptionUtility.isBaseMetricDescriptionSubsumedByMetricDescription(bmc1, x.getMetricDescription()) ||
			MetricDescriptionUtility.isBaseMetricDescriptionSubsumedByMetricDescription(bmc2, x.getMetricDescription()));
		
		if (bmc1.getName().equals(POINT_IN_TIME)) {
			var timestamp = MeasurementConverter.visitDataSeries(dataSeries1);
			return processDataSeries(timestamp, dataSeries2, filter);
		}
		else if (bmc2.getName().equals(POINT_IN_TIME)) {
			var timestamp = MeasurementConverter.visitDataSeries(dataSeries2);
			return processDataSeries(timestamp, dataSeries1, filter);
		} else {
			return null;
		}
		
	}
	
	
	private static MeasurementSet processDataSeries(List<Number> timestamps, DataSeries dataSeries, Predicate<? super MeasurementSpecification> filter) {
		var measureName = dataSeries.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMeasuringPoint().getStringRepresentation();
		var monitorRepo = Slingshot.getInstance().getInstance(MonitorRepository.class);
		var mpoint = dataSeries.getRawMeasurements().getMeasurementRange().getMeasurement().getMeasuringType().getMeasuringPoint();
		var monitor = monitorRepo.getMonitors().stream().filter(x -> 
			Objects.equal(x.getMeasuringPoint().getResourceURIRepresentation(), mpoint.getResourceURIRepresentation()))
				.findAny().orElse(null);
		var specification = monitor.getMeasurementSpecifications().stream()
				.filter(filter)
				.findAny().orElse(null);
		var values = MeasurementConverter.visitDataSeries(dataSeries);
		
		if (timestamps.size() != values.size())
			throw new IllegalStateException("Number of point in time values and measurments value do not match!");
		
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
		
		for (int j = 0; j < timestamps.size(); j++) {
			ms.add(new MeasurementSet.Measurement<Number>(values.get(j), timestamps.get(j).doubleValue()));
		}
		
		return ms;
	}
	
	
	

	/**
	 * Visit a data series and return the double values in an array list.
	 * 
	 * @param ds DataSeries with doubles
	 * @return ArrayList of Doubles
	 */
	private static List<Number> visitDataSeries(DataSeries ds) {
		var dao = (MeasurementsDao<Number, Duration>) MeasurementsUtility.<Duration>getMeasurementsDao(ds);

		var measures = dao.getMeasurements();
		
		var numbers = measures.stream().map(measure -> measure.getValue()).toList();

		try {
			dao.close();
		} catch (DataNotAccessibleException e) {
			e.printStackTrace();
		}

		return numbers;
	}

}
