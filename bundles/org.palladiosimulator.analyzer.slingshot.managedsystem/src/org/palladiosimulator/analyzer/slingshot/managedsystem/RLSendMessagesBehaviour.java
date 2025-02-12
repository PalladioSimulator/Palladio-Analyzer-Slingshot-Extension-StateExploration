package org.palladiosimulator.analyzer.slingshot.managedsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SystemDriver;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.managedsystem.converter.StateGraphConverter;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.Link;
import org.palladiosimulator.analyzer.slingshot.managedsystem.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.managedsystem.events.UtilityIntervalPassed;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.ManagedSystemFinishedMessage;
import org.palladiosimulator.analyzer.slingshot.managedsystem.messages.StateExploredEventMessage;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.CalculatorRegistered;
import org.palladiosimulator.analyzer.slingshot.networking.data.NetworkingConstants;
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
 * For RL only.
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = CalculatorRegistered.class, then = {})
@OnEvent(when = SimulationFinished.class, then = {})
@OnEvent(when = SimulationStarted.class, then = {})
@OnEvent(when = UtilityIntervalPassed.class, then = UtilityIntervalPassed.class)
public class RLSendMessagesBehaviour implements SimulationBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(RLSendMessagesBehaviour.class);

    final static int SLEEP_DELAY = 5000;

    private final SimuComConfig simuComConfig;

    /** Inputs for creating {@link StateGraphNode}s */
    private final MonitorRepository monitorRepo;
    private final ServiceLevelObjectiveRepository sloRepo;
    private ExperimentSetting expSetting = null;

    /** for slicing utility */
    private double prevUtilityTimestamp = 0.0;

    /** magic number that fixes the size of slices */
    private final double utilityInterval = 10.0;

    private final SystemDriver systemDriver;

    private final String clientName;

    private final Link linkToSystem;


    @Inject
    public RLSendMessagesBehaviour(final Link link, @Named(NetworkingConstants.CLIENT_NAME) final String clientName,
            final @Nullable SimuComConfig simuComConfig,
            final @Nullable MonitorRepository monitorRepo, final @Nullable ServiceLevelObjectiveRepository sloRepo) {

        this.clientName = clientName;

        this.simuComConfig = simuComConfig;
        this.monitorRepo = monitorRepo;
        this.sloRepo = sloRepo;

        this.systemDriver = Slingshot.getInstance().getSystemDriver();

        this.linkToSystem = link;
    }

    /**
     *
     * Publish the first {@link UtilityIntervalPassed} event at the beginning of the simulation.
     *
     * @param event
     * @return
     */
    @Subscribe
    public Result<UtilityIntervalPassed> onSimulationStarted(final SimulationStarted event) {
        return Result.of(new UtilityIntervalPassed(utilityInterval));
    }

    /**
     * When simulation is finished publish one last StateExploredMessage.
     */
    @Subscribe
    public void onSimulationFinished(final SimulationFinished event) {
        this.publishUtility(event);

        this.systemDriver.postEvent(new ManagedSystemFinishedMessage(clientName));
    }

    /**
     * After a given interval create a new {@link StateGraphNode} covering the past interval.
     *
     * The nodes should not overlap.
     */
    @Subscribe
    public Result<DESEvent> onUtilityIntervalPassed(final UtilityIntervalPassed event) {

        this.publishUtility(event);

        prevUtilityTimestamp = event.time();


        // Wait for next plan stepts.
        while (!linkToSystem.hasPlanArrived()) {
            try {
                LOGGER.info("Wait for initial next policies to apply.");
                Thread.sleep((long) Math.floor(SLEEP_DELAY));
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        final List<DESEvent> events = new ArrayList<>(linkToSystem.getEvents());

        events.add(event);

        return Result.of(events);
    }

    /**
     * Calculate utility of the given {@code event} by converting it to a {@link StateGraphNode} and
     * publish the utility with a {@link StateExploredEventMessage} via network.
     *
     * @param event
     */
    private void publishUtility(final DESEvent event) {
        assert this.expSetting != null : "ExperimentSettings are not yet set.";

        final StateGraphNode node = StateGraphConverter.convertState(this.monitorRepo, this.expSetting, this.sloRepo,
                prevUtilityTimestamp, event.time());

        this.systemDriver.postEvent(new StateExploredEventMessage(node, clientName));
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
