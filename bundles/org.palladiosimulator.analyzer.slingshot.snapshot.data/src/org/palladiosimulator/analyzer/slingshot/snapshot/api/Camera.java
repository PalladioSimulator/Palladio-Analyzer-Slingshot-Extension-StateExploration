package org.palladiosimulator.analyzer.slingshot.snapshot.api;

/**
 * This is the camera for taking the snapshot.
 *
 * (my mental image is like this : through my camera's viewfinder i watch stuff
 * change and sometime i release the shutter and get a picture of how stuff
 * looks at a certain point in time.)
 *
 *
 * @author Sophie Stieß
 *
 */
public interface Camera {

	/**
	 * ..and this is like releasing the shutter.
	 */
	public Snapshot takeSnapshot();
}
