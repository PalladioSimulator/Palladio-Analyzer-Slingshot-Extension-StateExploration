package org.palladiosimulator.analyzer.slingshot.managedsystem.utility.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.converter.StateGraphConverter;
import org.palladiosimulator.analyzer.slingshot.converter.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.converter.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.managedsystem.events.UtilityIntervalPassed;
import org.palladiosimulator.analyzer.slingshot.managedsystem.utility.plotter.CSVCreator;
import org.palladiosimulator.analyzer.slingshot.managedsystem.utility.plotter.CSVCreator.DataPoint;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.CalculatorRegistered;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.edp2.models.Repository.Repository;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

/**
 *
 * Behaviour to inject reconfigurations into a simulation run according to a provided plan.
 *
 * @note not in use anymore, see {@link UtilityCalculatorModule#configure(com.google.inject.Binder)}
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = CalculatorRegistered.class, then = {})
@OnEvent(when = SimulationFinished.class, then = {})
@OnEvent(when = SimulationStarted.class, then = {})
@OnEvent(when = UtilityIntervalPassed.class, then = UtilityIntervalPassed.class)
public class UtilityCalculatorAndPlotterBehaviour implements SimulationBehaviorExtension {

    /** String constants for filenames and directories */
    private static final String directoryPrefix = "tmp";
    private static final String utilityFilename = "utility.csv";
    private static final String aggregatedUtilityFilename = "cummulativeUtility.csv";

    /** String constants for axes and labels */
    private static final String pointInTimeHeader = "point in time [s]";
    private static final String utilityHeader = "utility of slice";
    private static final String aggregatedUtilityHeader = "aggregated utility";

    private final static Logger LOGGER = Logger.getLogger(UtilityCalculatorAndPlotterBehaviour.class);
    private final SimuComConfig simuComConfig;

    /** Inputs for creating {@link StateGraphNode}s */
    private final MonitorRepository monitorRepo;
    private final ServiceLevelObjectiveRepository sloRepo;
    private ExperimentSetting expSetting = null;

    /** for slicing utility */
    private double prevUtilityTimestamp = 0.0;

    /** magic number that fixe the size of slices */
    private final double utilityInterval = 10.0;

    /** sliced nodes */
    private final List<StateGraphNode> nodes = new ArrayList<>();

    @Inject
    public UtilityCalculatorAndPlotterBehaviour(final @Nullable SimuComConfig simuComConfig,
            final @Nullable MonitorRepository monitorRepo, final @Nullable ServiceLevelObjectiveRepository sloRepo) {
        this.simuComConfig = simuComConfig;
        this.monitorRepo = monitorRepo;
        this.sloRepo = sloRepo;
    }

    /**
     * When simulation is finished, save all measurements, as well as utility and aggregated utility
     * to .csv file.
     */
    @Subscribe
    public void onSimulationFinished(final SimulationFinished event) {
        assert this.expSetting != null : "ExperimentSettings are not yet set.";

        final StateGraphNode node = StateGraphConverter.convertState(Optional.of(this.monitorRepo), this.expSetting, Optional.of(this.sloRepo),
                prevUtilityTimestamp, event.time(), "", "", List.of());
        nodes.add(node);

        persistUtilityData();
        persistAggregatedUtilityData();

        final StateGraphNode totalNode = StateGraphConverter.convertState(Optional.of(this.monitorRepo), this.expSetting,
                Optional.of(this.sloRepo), 0, event.time(), "", "", List.of());

        persistOtherMeasurementData(totalNode);
    }

    /**
     * Persist utility data from nodes sliced during simulation to *.csv file.
     */
    private void persistUtilityData() {
        final List<DataPoint> utilityData = new ArrayList<>();

        nodes.forEach(n -> utilityData.add(new DataPoint(n.endTime(), n.utility()
            .getTotalUtilty())));

        final CSVCreator creatorUtility = new CSVCreator(directoryPrefix, utilityFilename);
        creatorUtility.write(utilityData, pointInTimeHeader, utilityHeader);
    }

    /**
     * Persist aggregated utility data from nodes sliced during simulation to *.csv file.
     *
     * The aggregated data is calculated by summing up all previous data points.
     */
    private void persistAggregatedUtilityData() {
        final List<DataPoint> aggregatedUtilityData = new ArrayList<>();

        aggregatedUtilityData.add(new DataPoint(0, 0));

        nodes.forEach(n -> aggregatedUtilityData.add(new DataPoint(n.endTime(), n.utility()
            .getTotalUtilty()
                + aggregatedUtilityData.get(aggregatedUtilityData.size() - 1)
                    .y())));

        final CSVCreator creatorCummulativeUtility = new CSVCreator(directoryPrefix, aggregatedUtilityFilename);
        creatorCummulativeUtility.write(aggregatedUtilityData, pointInTimeHeader, aggregatedUtilityHeader);
    }

    /**
     * Persist all measurements data captured in the given {@link StateGraphNode} to *.csv file.
     *
     * @param node
     *            state to persist data from.
     */
    private void persistOtherMeasurementData(final StateGraphNode node) {
        for (final MeasurementSet set : node.measurements()) {

            final CSVCreator creator = new CSVCreator(directoryPrefix, createCleanFilenameString(set));

            final List<DataPoint> data = new ArrayList<>();
            set.getElements()
                .forEach(e -> data.add(new DataPoint(e.timeStamp(), e.measure()
                .doubleValue())));

            creator.write(data, pointInTimeHeader, set.getMetricName());
        }
    }

    /**
     * Create a clean file name based on the given {@link MeasurementSet}.
     *
     * Cleaning includes:
     * <li>removing whitspaces
     * <li>removing ":"
     * <li>removing the "[TRANSIENT]" hint
     * <li><i> further items to be added as they become problematic </i>
     *
     * @param set
     * @return
     */
    private String createCleanFilenameString(final MeasurementSet set) {
        final String initialName = set.getSpecificationName() + "_" + set.getMonitorName() + ".csv";

        final String fileName = initialName.replace(" ", "")
            .replace(":", "")
            .replace("[TRANSIENT]", "");
        return fileName;
    }

    /**
     * After a given interval create a new {@link StateGraphNode} covering the past interval.
     *
     * The nodes should not overlap.
     */
    @Subscribe
    public Result<UtilityIntervalPassed> onUtilityIntervalPassed(final UtilityIntervalPassed event) {
        assert this.expSetting != null : "ExperimentSettings are not yet set.";

        final StateGraphNode node = StateGraphConverter.convertState(Optional.of(this.monitorRepo), this.expSetting, Optional.of(this.sloRepo),
                prevUtilityTimestamp, event.time(), "", "", List.of());

        nodes.add(node);

        prevUtilityTimestamp = event.time();

        return Result.of(event);
    }

    @Subscribe
    public Result<UtilityIntervalPassed> onSimulationStarted(final SimulationStarted event) {
        return Result.of(new UtilityIntervalPassed(utilityInterval));
    }

    /**
     * Extract the {@link ExperimentSetting}s for the current simulation run.
     *
     * This may short circuit after the first run. (But does not right now.)
     *
     * @param calculatorRegistered
     *            even it self not used, only trigger to know that experiment setting were updated
     *            with a new calculator.
     */
    @Subscribe
    public void onCalculatorRegistered(final CalculatorRegistered calculatorRegistered) {

        final List<Repository> repos = RepositoryManager.getCentralRepository()
            .getAvailableRepositories();

        final Optional<Repository> repo = repos.stream()
            .filter(r -> !r.getExperimentGroups()
                .isEmpty())
            .findFirst();

        if (repo.isEmpty()) {
            throw new IllegalStateException("Repository is missing.");
        }

        final List<ExperimentGroup> groups = repo.get()
            .getExperimentGroups()
            .stream()
            .filter(g -> g.getPurpose()
                .equals(this.simuComConfig.getNameExperimentRun()))
            .collect(Collectors.toList());

        if (groups.size() != 1) {
            throw new IllegalStateException(
                    String.format("Wrong number of matching Experiment Groups. should be 1 but is %d", groups.size()));
        }

        final List<ExperimentSetting> settings = groups.get(0)
            .getExperimentSettings()
            .stream()
            .filter(s -> s.getDescription()
                .equals(this.simuComConfig.getVariationId()))
            .collect(Collectors.toList());

        if (settings.size() != 1) {
            throw new IllegalStateException(String.format(
                    "Wrong number of Experiment Settings matching the variation id. should be 1 but is %d",
                    settings.size()));
        }

        this.expSetting = settings.get(0);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
