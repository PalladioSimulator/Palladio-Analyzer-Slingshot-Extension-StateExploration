package org.palladiosimulator.analyzer.slingshot.injection.utility.plotter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelChange;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ResourceEnvironmentChange;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationFinished;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.injection.utility.plotter.CSVCreator.DataPoint;


/**
 *
 * Behaviour to inject reconfigurations into a simulation run according to a provided plan.
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = ModelAdjusted.class, then = {})
@OnEvent(when = SimulationFinished.class, then = {})
public class LogAdjustemntsBehaviour implements SimulationBehaviorExtension {

    private final static Logger LOGGER = Logger.getLogger(LogAdjustemntsBehaviour.class);

    /** String constants for filenames, directories, axes and labels */
    private static final String directoryPrefix = "tmp";
    private static final String filename = "numberOfRecourceconainer.csv";
    private static final String pointInTimeHeader = "point in time [s]";
    private static final String utilityHeader = "# resource container";

    private final List<DataPoint> data = new ArrayList<>();

    /**
     * Constructor.
     */
    public LogAdjustemntsBehaviour() {
        super();

    }

    /**
     * After each model adjustment, add the new number of containers to the data.
     */
    @Subscribe
    public void onModelAdjusted(final ModelAdjusted event) {
        for (final ModelChange<?> change : event.getChanges()) {
            if (change instanceof final ResourceEnvironmentChange resChange) {
                final int old = resChange.getOldResourceContainers()
                    .size();
                final int deleted = resChange.getDeletedResourceContainers()
                    .size();
                final int added = resChange.getNewResourceContainers()
                    .size();

                if (deleted + added != 0) {
                    data.add(new DataPoint(event.time(), old));
                    data.add(new DataPoint(event.time(), (old + added - deleted)));
                }
            }
        }
    }

    /**
     * Once the simulation is finished persist the adjustment data to *.csv
     *
     * WAIT. there's a measuring point for this, or is there?
     *
     * @param event
     *            announces the end of the simulation
     */
    @Subscribe
    public void onSimulationFinished(final SimulationFinished event) {
        (new CSVCreator(directoryPrefix, filename)).write(data, pointInTimeHeader, utilityHeader);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
