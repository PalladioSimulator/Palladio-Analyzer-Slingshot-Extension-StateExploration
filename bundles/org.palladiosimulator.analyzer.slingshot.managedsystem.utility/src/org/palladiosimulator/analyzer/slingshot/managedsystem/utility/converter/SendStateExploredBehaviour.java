package org.palladiosimulator.analyzer.slingshot.managedsystem.utility.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.managedsystem.untility.converter.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.managedsystem.untility.converter.triggerevent.UtilityIntervalPassed;
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
 * Behaviour to send messages to Advise.
 *
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = CalculatorRegistered.class, then = {})
@OnEvent(when = SimulationFinished.class, then = {})
@OnEvent(when = SimulationStarted.class, then = {})
@OnEvent(when = UtilityIntervalPassed.class, then = UtilityIntervalPassed.class)
public class SendStateExploredBehaviour implements SimulationBehaviorExtension {


    private final static Logger LOGGER = Logger.getLogger(SendStateExploredBehaviour.class);
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
    public SendStateExploredBehaviour(final @Nullable SimuComConfig simuComConfig,
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

        final StateGraphNode node = StateGraphConverter.convertState(this.monitorRepo, this.expSetting, this.sloRepo,
                prevUtilityTimestamp, event.time());
        nodes.add(node);


        final StateGraphNode totalNode = StateGraphConverter.convertState(this.monitorRepo, this.expSetting,
                this.sloRepo, 0, event.time());

        // this.systemDriver.postEvent(new StateExploredEventMessage(node));

    }

    /**
     * After a given interval create a new {@link StateGraphNode} covering the past interval.
     *
     * The nodes should not overlap.
     */
    @Subscribe
    public Result<UtilityIntervalPassed> onUtilityIntervalPassed(final UtilityIntervalPassed event) {
        assert this.expSetting != null : "ExperimentSettings are not yet set.";

        final StateGraphNode node = StateGraphConverter.convertState(this.monitorRepo, this.expSetting, this.sloRepo,
                prevUtilityTimestamp, event.time());

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
