package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.User;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.simulation.events.AbstractEntityChangedEvent;
import org.palladiosimulator.analyzer.slingshot.simulation.events.ModelPassedEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.EventRecord;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;

public class InMemoryRecord implements EventRecord {
	private static final Logger LOGGER = Logger.getLogger(InMemoryRecord.class);

	public InMemoryRecord() {
		this.openCalculators = new HashMap<>();
		this.openJob = new HashMap<>();
	}
	
	
	/* states that still change due to running simulation */
	private final Map<User, ArrayDeque<ModelPassedEvent<?>>> openCalculators;
	private final Map<User, JobInitiated> openJob;

	private void internalUpdateRecord(final AbstractEntityChangedEvent<?> event) {
		if (event instanceof UsageModelPassedElement<?>) {
			final User user = ((UsageModelPassedElement<?>) event).getContext().getUser();
			final Object passedElement = ((UsageModelPassedElement<?>) event).getModelElement();

			if (!openCalculators.containsKey(user)) {
				openCalculators.put(user, new ArrayDeque<>());
			}

			if (passedElement instanceof Start) {
				openCalculators.get(user).push((ModelPassedEvent<?>)event);
			} else if (passedElement instanceof Stop) {
				openCalculators.get(user).pop();
			}
			return;
		}
		if (event instanceof JobInitiated) {
			final User user = ((JobInitiated) event).getEntity().getRequest().getUser();
			openJob.put(user, ((JobInitiated) event));
			return;
		}
		if (event instanceof JobFinished) {
			final User user = ((JobFinished) event).getEntity().getRequest().getUser();
			openJob.remove(user);
			return;
		}
	}

	public void addInitiatedCalculator (final UsageModelPassedElement<Start> event) {
		final User user = event.getContext().getUser();
		if (!openCalculators.containsKey(user)) {
			openCalculators.put(user, new ArrayDeque<>());
		}
		openCalculators.get(user).push(event);
	}

	public void removeFinishedCalculator (final UsageModelPassedElement<Stop> event) {
		final User user = event.getContext().getUser();
		if (openCalculators.containsKey(user)) {
			openCalculators.get(user).pop();
		}
	}

	public void addInitiatedJob (final JobInitiated event) {
		openJob.put(event.getEntity().getRequest().getUser(), event);
	}
	

	public void removeFinishedJob (final JobFinished event) {
		openJob.remove(event.getEntity().getRequest().getUser());
	}

	public void clear() {
		this.openCalculators.clear();
		this.openJob.clear();
	}

	@Override
	public void updateRecord(AbstractEntityChangedEvent<?> event) {
		this.internalUpdateRecord(event);
		
		if (event instanceof JobInitiated) {
			this.addInitiatedJob((JobInitiated)event);
		} else if (event instanceof JobFinished) {
			this.removeFinishedJob((JobFinished)event);
		} else if (event instanceof UsageModelPassedElement<?>) {
			Object modelElement = ((UsageModelPassedElement<?>) event).getModelElement();
			
			if (modelElement instanceof Start) {
				this.addInitiatedCalculator((UsageModelPassedElement<Start>) event);
			} else if (modelElement instanceof Stop) {
				this.removeFinishedCalculator((UsageModelPassedElement<Stop> )event);
			}
		}
	}

	@Override
	public Set<AbstractEntityChangedEvent<?>> getRecord() {
		Set<AbstractEntityChangedEvent<?>> rval = new HashSet<>();
		openCalculators.values().stream().forEach(adq -> rval.addAll(adq));
		rval.addAll(openJob.values());
		return rval;
	}
}
