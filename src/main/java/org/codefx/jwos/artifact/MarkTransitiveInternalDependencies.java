package org.codefx.jwos.artifact;

import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Marks the internal dependencies of an artifact and its dependees.
 */
public enum MarkTransitiveInternalDependencies {

	/**
	 * The analysis could not be performed for this artifact or one of its dependees.
	 */
	UNKNOWN,

	/**
	 * The analysis was performed and no internal dependencies were found.
	 */
	NONE,

	/**
	 * The analysis was performed and internal dependencies were found
	 * for at least one of the dependees.
	 */
	INDIRECT,

	/**
	 * The analysis was performed and internal dependencies were found
	 * for this artifact.
	 */
	DIRECT;

	public static MarkTransitiveInternalDependencies from(MarkInternalDependencies marker) {
		switch (marker) {
			case UNKNOWN:
				return UNKNOWN;
			case NONE:
				return NONE;
			case DIRECT:
				return DIRECT;
			default:
				throw unknownMarker(marker);
		}
	}

	/**
	 * @param marker internal dependencies of the artifact for which this is computed
	 * @param dependees an empty {@link Optional} if dependee resolution failed;
	 *                  otherwise a possibly empty stream of their transitive markers
	 */
	public static MarkTransitiveInternalDependencies fromDependees(
			MarkInternalDependencies marker,
			Optional<Stream<MarkTransitiveInternalDependencies>> dependees) {
		return dependees
				.map(deps -> forResolvedDependees(marker, deps))
				.orElseGet(() -> forUnresolvedDependees(marker));
	}

	private static MarkTransitiveInternalDependencies forResolvedDependees(
			MarkInternalDependencies marker, Stream<MarkTransitiveInternalDependencies> dependees) {
		return forArtifactAndCombinedDependeeMarker(
				marker,
				dependees.reduce(NONE, MarkTransitiveInternalDependencies::combineDependees));
	}

	private static MarkTransitiveInternalDependencies combineDependees(
			MarkTransitiveInternalDependencies one, MarkTransitiveInternalDependencies other) {
		if (one == DIRECT || other == DIRECT)
			return DIRECT;
		if (one == INDIRECT || other == INDIRECT)
			return INDIRECT;
		if (one == UNKNOWN || other == UNKNOWN)
			return UNKNOWN;
		return NONE;
	}

	private static MarkTransitiveInternalDependencies forArtifactAndCombinedDependeeMarker(
			MarkInternalDependencies marker, MarkTransitiveInternalDependencies dependeeMarker) {
		switch (marker) {
			case UNKNOWN:
				// This happens if jdeps analysis failed, which is a rare case.
				// While we can technically never say anything but UNKNOWN about the transitive result,
				// we opt on the side of reporting what we know over being 100% exact.
				// So as soon as the artifact is problematic because one of its dependencies is, we report that.
				switch (dependeeMarker) {
					case UNKNOWN:
						return UNKNOWN;
					case NONE:
						return UNKNOWN;
					case INDIRECT:
						return INDIRECT;
					case DIRECT:
						return INDIRECT;
					default:
						throw unknownMarker(dependeeMarker);
				}
			case NONE:
				switch (dependeeMarker) {
					case UNKNOWN:
						return UNKNOWN;
					case NONE:
						return NONE;
					case INDIRECT:
						return INDIRECT;
					case DIRECT:
						// if the dependee has direct dependencies, that this artifact his indirect dependencies
						return INDIRECT;
					default:
						throw unknownMarker(dependeeMarker);
				}
			case DIRECT:
				// if the marked artifact has direct dependencies, the dependees do not matter for the final result
				return DIRECT;
			default:
				throw unknownMarker(marker);
		}
	}

	private static MarkTransitiveInternalDependencies forUnresolvedDependees(MarkInternalDependencies marker) {
		switch (marker) {
			case UNKNOWN:
				return UNKNOWN;
			case NONE:
				return UNKNOWN;
			case DIRECT:
				return DIRECT;
			default:
				throw unknownMarker(marker);
		}
	}

	private static IllegalArgumentException unknownMarker(MarkInternalDependencies marker) {
		return unknownMarker(marker.toString());
	}

	private static IllegalArgumentException unknownMarker(MarkTransitiveInternalDependencies marker) {
		return unknownMarker(marker.toString());
	}

	private static IllegalArgumentException unknownMarker(String marker) {
		return new IllegalArgumentException(format("Unknown internal dependency marker \"%s\".", marker));
	}

}
