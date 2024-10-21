package org.palladiosimulator.analyzer.slingshot.injection.utility.converter;

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
import org.palladiosimulator.analyzer.slingshot.injection.utility.converter.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.injection.utility.converter.triggerevent.UtilityIntervalPassed;
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
 * @author Sophie Stieß
 *
 */
@OnEvent(when = CalculatorRegistered.class, then = {})
@OnEvent(when = SimulationFinished.class, then = {})
@OnEvent(when = SimulationStarted.class, then = {})
@OnEvent(when = UtilityIntervalPassed.class, then = UtilityIntervalPassed.class)
public class UtilityCalculatorBehaviour implements SimulationBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(UtilityCalculatorBehaviour.class);
    private final SimuComConfig simuComConfig;

    private ExperimentSetting expSetting;

    private final MonitorRepository monitorRepo;

    private final ServiceLevelObjectiveRepository sloRepo;

    private double prevUtilityTimestamp = 0.0;

    /** magic number */
    private final double utilityInterval = 10.0;

    private final List<StateGraphNode> nodes = new ArrayList<>();

    @Inject
    public UtilityCalculatorBehaviour(final @Nullable SimuComConfig simuComConfig,
            final @Nullable MonitorRepository monitorRepo, final @Nullable ServiceLevelObjectiveRepository sloRepo) {
        this.simuComConfig = simuComConfig;
        this.monitorRepo = monitorRepo;
        this.sloRepo = sloRepo;
    }

    /**
     * Announce start of simulation.
     */
    @Subscribe
    public void onSimulationFinished(final SimulationFinished event) {
        final StateGraphNode node = StateGraphConverter.convertState(this.monitorRepo, this.expSetting, this.sloRepo,
                prevUtilityTimestamp, event.time());
        // logUtility(node);
        nodes.add(node);
        logPlotUtility();
    }


    /**
     * Announce start of simulation.
     */
    @Subscribe
    public Result<UtilityIntervalPassed> onIntervallPassed(final UtilityIntervalPassed event) {
        final StateGraphNode node = StateGraphConverter.convertState(this.monitorRepo, this.expSetting, this.sloRepo,
                prevUtilityTimestamp, event.time());
        // logUtility(node);

        nodes.add(node);

        prevUtilityTimestamp = event.time();

        return Result.of(event);
    }

    @Subscribe
    public Result<UtilityIntervalPassed> onSimulationStarted(final SimulationStarted event) {
        return Result.of(new UtilityIntervalPassed(utilityInterval));
    }

    /**
     *
     * @param node
     */
    private void logUtility(final StateGraphNode node) {
        node.utility()
            .getData()
            .forEach(d -> LOGGER.error(d.type()
                .toString() + " : " + d.id() + " : " + d.utility()));
        LOGGER.error(String.format("Utility of the Node is %f", node.utility()
            .getTotalUtilty()));
    }

    /**
     *
     * @param node
     */
    private void logPlotUtility() {
        for (final StateGraphNode node : nodes) {
            LOGGER.error(String.format("%f %f", node.endTime(), node.utility()
                .getTotalUtilty()));
        }
    }

    /**
     *
     * @param calculatorRegistered
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
