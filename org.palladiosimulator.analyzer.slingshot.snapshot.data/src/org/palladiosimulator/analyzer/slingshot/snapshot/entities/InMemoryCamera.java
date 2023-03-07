package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.ClosedWorkloadUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelper;
import org.palladiosimulator.analyzer.slingshot.behavior.util.CloneHelperWithVisitor;
import org.palladiosimulator.analyzer.slingshot.behavior.util.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.simulation.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.simulation.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Camera;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.EventRecord;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;

import com.google.common.base.Preconditions;

public final class InMemoryCamera implements Camera {
	private static final Logger LOGGER = Logger.getLogger(InMemoryCamera.class);

	private final EventRecord record;
	private final SimulationEngine engine;
	private final Allocation allocation;
	private final MonitorRepository monitorRepository;

	private final ResourceEnvironment resourceEnvironment;
	private final System system;

	private final LambdaVisitor<DESEvent, DESEvent> adjustOffset;

	public InMemoryCamera(final EventRecord record, final SimulationEngine engine, final Allocation allocation, final MonitorRepository monitorRepository) {
		this.record = record;
		this.engine = engine;
		this.allocation = allocation;
		this.resourceEnvironment = allocation.getTargetResourceEnvironment_Allocation();
		this.system = allocation.getSystem_Allocation();

		this.monitorRepository = monitorRepository;

		this.adjustOffset = new LambdaVisitor<DESEvent, DESEvent>()
				.on(UsageModelPassedElement.class).then(this::clone)
				.on(ClosedWorkloadUserInitiated.class).then(this::clone)
				.on(DESEvent.class).then(e -> e);
	}

	@Override
	public Snapshot takeSnapshot(final double pointInTime) {
		final Snapshot snapshot = new InMemorySnapshot(snapEvents());
		//snapshot.setAllocation(this.snapAllocation(snapshot.getId()));
		//snapshot.setMonitorRepository(this.snapMonitorRepository(snapshot.getId()));
		return snapshot;
	}

	private DESEvent clone(final UsageModelPassedElement<?> event) {
		return (new CloneHelper()).clone(event, engine.getTime());
	}
	private DESEvent clone(final ClosedWorkloadUserInitiated event) {
		return (new CloneHelper()).clone(event, engine.getTime());
	}

	/**
	 * TODO
	 *
	 * @param pointInTime
	 * @return
	 */
	private Set<DESEvent> snapEvents() {
		final Set<DESEvent> relevantEvents = engine.getScheduledEvents();
		relevantEvents.addAll(record.getRecord());

		final Set<DESEvent> offsettedEvents = relevantEvents.stream().map(adjustOffset).collect(Collectors.toSet());
		final Set<DESEvent> clonedEvents = (new CloneHelperWithVisitor()).clone(offsettedEvents);

		return clonedEvents;
	}

	private MonitorRepository snapMonitorRepository(final String idSegment) {
		return this.monitorRepository;
	}

	/**
	 *
	 * @param idSegment
	 * @return
	 */
	private Allocation snapAllocation(final String idSegment) {
		// ONLY WORKS AFTER PROXIE RESOLUTION.
		Preconditions.checkState(this.resourceEnvironment.equals(this.allocation.getTargetResourceEnvironment_Allocation()), "inconsistent camera wrt. res env.");
		Preconditions.checkState(this.system.equals(this.allocation.getSystem_Allocation()), "inconsistent camera wrt. system.");


		final URI oldAllocUri = this.allocation.eResource().getURI();
		final URI newAllocUri = ResourceUtils.insertFragment(oldAllocUri, idSegment, oldAllocUri.segmentCount()-1);
		this.allocation.eResource().setURI(newAllocUri);

		final URI oldResUri = this.resourceEnvironment.eResource().getURI();
		final URI newResUri = ResourceUtils.insertFragment(oldResUri, idSegment, oldResUri.segmentCount()-1);
		this.resourceEnvironment.eResource().setURI(newResUri);

		final URI oldSysUri = this.system.eResource().getURI();
		final URI newSysUri = ResourceUtils.insertFragment(oldSysUri, idSegment, oldSysUri.segmentCount()-1);
		this.system.eResource().setURI(newSysUri);


		ResourceUtils.saveResource(this.resourceEnvironment.eResource());
		ResourceUtils.saveResource(this.system.eResource());
		ResourceUtils.saveResource(this.allocation.eResource());

		this.allocation.eResource().setURI(oldAllocUri);
		this.system.eResource().setURI(oldSysUri);
		this.resourceEnvironment.eResource().setURI(oldResUri);


		final ResourceSet set = new ResourceSetImpl();
		final Resource res = set.createResource(newAllocUri);

		try {
			res.load(((XMLResource) res).getDefaultLoadOptions());
		} catch (final IOException e) {
			e.printStackTrace();
		}


		final String allocFragment = res.getURIFragment(this.allocation);
		final Allocation copy = (Allocation) res.getEObject(allocFragment);

		return copy;
	}


	/**
	 *
	 * Does not work as we copy *old* references....ugh.
	 *
	 * @param idSegment
	 * @return
	 */
	private Allocation snapAllocationWithCopy(final String idSegment) {
		Preconditions.checkState(this.resourceEnvironment.equals(this.allocation.getTargetResourceEnvironment_Allocation()), "inconsistent camera wrt. res env.");
		Preconditions.checkState(this.system.equals(this.allocation.getSystem_Allocation()), "inconsistent camera wrt. system.");
		final ResourceEnvironment copyResEnv = EcoreUtil.copy(this.resourceEnvironment);
		final System copySyS = EcoreUtil.copy(this.system);
		final Allocation copyAlloc = EcoreUtil.copy(this.allocation);
		//this copies all the element native to the model, but references to other model obviously are copied as they are...

		copyAlloc.setSystem_Allocation(copySyS);
		copyAlloc.setTargetResourceEnvironment_Allocation(copyResEnv);
		//this only sets the 'outer' reference but does not update the model elements referenced by the allocationContexts :/

		final ResourceSet set = new ResourceSetImpl();

		saveCopy(this.resourceEnvironment.eResource().getURI(), idSegment, set, copyResEnv);
		saveCopy(this.system.eResource().getURI(), idSegment, set, copySyS);
		saveCopy(this.allocation.eResource().getURI(), idSegment, set, copyAlloc);

		return copyAlloc;
	}

	/**
	 * this breaks once i start to snapshot initialized runs, cause in the initialized runs, the uri is not the base uri any more D:
	 *
	 * actually, this already is broken O_O
	 * im pretty sure though, that i work when i used the old resource / resource set...
	 *
	 * @param originalUri
	 * @param id
	 * @param set
	 * @param copy
	 */
	private void saveCopy(final URI originalUri, final String id, final ResourceSet set, final EObject copy) {
		final URI uri = ResourceUtils.insertFragment(originalUri, id, originalUri.segmentCount()-1);
		final Resource res = set.createResource(uri);
		res.getContents().add(copy);
		ResourceUtils.saveResource(res);
	}

	//private MonitorRepsitory snapMonitorRepository() {
	//	return null;
	//}
}
