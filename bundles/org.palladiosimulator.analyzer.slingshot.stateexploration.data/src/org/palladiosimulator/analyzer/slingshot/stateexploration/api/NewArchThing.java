package org.palladiosimulator.analyzer.slingshot.stateexploration.api;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 *
 * Purposed is creating a copy of our PCM models in the results folder, and then using those. 
 *
 *
 * @author Sarah Stieß
 *
 */
public class NewArchThing {

	private static final Logger LOGGER = Logger.getLogger(NewArchThing.class.getName());

	/**
	 *
	 * Create a copy of the models in the provided resource set at the provided
	 * location.
	 *
	 * @param location location of the root node's architecture configuration.
	 * @param set      resources set with models
	 * @return a new {@code UriAndSetBasedArchitectureConfiguration}.
	 */
	public static void copyModelsForRoot(final ResourceSet set, final URI resultFolder) {
		String cleanLocation = resultFolder.toString();

		if (resultFolder.hasTrailingPathSeparator()) {
			cleanLocation = cleanLocation.substring(0, cleanLocation.length() - 1);
		}

		// 1. ensure that all models are loaded.
		EcoreUtil.resolveAll(set);

		final List<Resource> whitelisted = ArchitectureConfigurationUtil.getWhitelistedResources(set);

		// 2. update paths
		for (final Resource resource : whitelisted) {
			final String file = resource.getURI().lastSegment();

			final URI newUri = URI.createURI(cleanLocation)/*.appendSegment(explorationId).appendSegment(nextIdSegment)*/
					.appendSegment(file);
			resource.setURI(newUri);
		}

		// 3. save to new path (thereby create a copy)
		ArchitectureConfigurationUtil.saveWhitelisted(set);
	}

	/**
	 * Fills the {@code uris} map of this {@code ArchitectureConfiguration}.
	 *
	 * Ensures, that {@code uris} contains a mapping for all white listed model
	 * classes.
	 *
	 * @param set ResourceSet to be filled into {@code uris}.
	 * @throws IllegalArgumentException if any Resource in the given ResourceSet is
	 *                                  empty.
	 */
//	private static Map<EClass, URI> createUriMap(final ResourceSet set) {
//		final Map<EClass, URI> map = new HashMap<>();
//
//		for (final Resource resource : set.getResources()) {
//			if (resource.getContents().isEmpty()) {
//				LOGGER.info(String.format("Empty resource for : %s.", resource.getURI().toString()));
//			} else if (ArchitectureConfigurationUtil.MODEL_ECLASS_WHITELIST
//					.contains(resource.getContents().get(0).eClass())) {
//				map.put(resource.getContents().get(0).eClass(), resource.getURI());
//			}
//		}
//
//		return map;
//	}

	/**
	 * Creates a copy of this architecture Configuration.
	 *
	 * The copy is created by saving all models of this configuration to a new
	 * location in the file system, and setting the EClass to URI mappings of the
	 * copy such that they are pointing to the copied model in the file system.
	 *
	 */
//	public NewArchThing copy() {
//
//		final String nextIdSegment = UUID.randomUUID().toString();
//
//		this.load(); // 1. ensure that load all models are loaded.
//
//		final List<Resource> whitelisted = ArchitectureConfigurationUtil.getWhitelistedResources(this.set);
//		final Map<EClass, URI> copyUris = new HashMap<>();
//
//		// 2. update paths
//		for (final Resource resource : whitelisted) {
//			// cache?
//			final URI oldUri = resource.getURI();
//			final URI newUri = ResourceUtils.replaceFragment(oldUri, nextIdSegment, oldUri.segmentCount() - 2);
//			resource.setURI(newUri);
//			copyUris.put(resource.getContents().get(0).eClass(), newUri);
//		}
//
//		// 3. save to new path
//		ArchitectureConfigurationUtil.saveWhitelisted(this.set);
//
//		// 4. reset URIs to old values.
//		for (final Resource resource : whitelisted) {
//			final URI oldUri = uris.get(resource.getContents().get(0).eClass());
//			resource.setURI(oldUri);
//		}
//
//		// 7. build copy with copied models
//		return new NewArchThing(copyUris, nextIdSegment);
//	}
}
