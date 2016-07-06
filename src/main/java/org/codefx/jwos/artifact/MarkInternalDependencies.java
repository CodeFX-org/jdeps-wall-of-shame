package org.codefx.jwos.artifact;

/**
 * Marks the internal dependencies of an individual artifact.
 */
enum MarkInternalDependencies {

	/**
	 * The jdeps analysis could not be performed.
	 */
	UNKNOWN,

	/**
	 * The analysis was performed and no internal dependencies were found.
	 */
	NONE,

	/**
	 * The analysis was performed and internal dependencies were found.
	 */
	DIRECT;

}
