package org.palladiosimulator.analyzer.slingshot.stateexploration.change.api;

/**
 * 
 * The actual change. Not to be confused with the ChangeApplicator.
 * 
 * imho, we should rename this class. maybe something akin to "AppliedChange"?
 * Just to indicate, that is not about the action of applying (i.e. an
 * application) but about the .. aftermath of the application? i.e. the things
 * that actually changed.
 * 
 * Yeah fuck it, i just gonna rename it.
 * 
 * This is kinda the shared parent for all applied changes, i.e. Env or
 * reconfiguration. And significant changes in measurements. and then "applied"
 * is again the wrong word, because we do not apply changes in measurements,
 * they just occur.
 * 
 * @author stiesssh
 *
 */
public interface Change {

	// getChangeType()
	// getOldModelElements() // but you could also calculate the diff og source and target state.
	// getTargetModelElements()
}
