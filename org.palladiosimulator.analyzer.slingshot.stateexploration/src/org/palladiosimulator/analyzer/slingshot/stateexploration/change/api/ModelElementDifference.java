package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

import java.util.Optional;

import org.palladiosimulator.pcm.core.entity.Entity;

/**
 *
 * The idea : according to the domain model, the actual applied change should know about the changed PCM instance (c.f. Binding)
 *
 * Difficulties:
 * - EnvChange and Reconfiguration change different PCM elements (allocation v.s something Usage related).
 * - Yet change PCM elements is common for both changes, thus i want a uniform access in the interface.
 * - changes may be delete, add or just a change inside the PCM element (e.g. updated interarrivalrate).
 *
 * Thus, i created this class which can represent deletes, adds and changes for any kind of PCM Element.
 *
 * Actually, everyone can still get the difference by looking at the models of start and end state of the transition.
 *
 *
 */
public class ModelElementDifference<T extends Entity> {

	private final Optional<T> oldElement;
	private final Optional<T> newElement;

	public ModelElementDifference(final Optional<T> oldElement, final Optional<T> newElement) {
		super();
		this.oldElement = oldElement;
		this.newElement = newElement;
	}

	public Optional<T> getOldElement() {
		return oldElement;
	}

	public Optional<T> getNewElement() {
		return newElement;
	}
}
