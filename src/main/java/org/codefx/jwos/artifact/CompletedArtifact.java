package org.codefx.jwos.artifact;

import com.google.common.collect.ImmutableSet;
import javaslang.control.Either;
import org.codefx.jwos.jdeps.dependency.Violation;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * An artifact that has undergone all processing steps (either successfully or not).
 */
public class CompletedArtifact implements IdentifiesArtifact {

	private final ArtifactCoordinates artifact;
	private final MarkTransitiveInternalDependencies transitiveMarker;
	private final Either<Exception, ImmutableSet<Violation>> violations;
	private final Either<Exception, ImmutableSet<CompletedArtifact>> dependees;

	// CONSTRUCTION

	private CompletedArtifact(
			ArtifactCoordinates artifact, Either<Exception, ImmutableSet<Violation>> violations,
			Either<Exception, ImmutableSet<CompletedArtifact>> dependees) {
		this.artifact = artifact;
		this.violations = violations;
		this.dependees = dependees;
		this.transitiveMarker = MarkTransitiveInternalDependencies
				.fromDependees(determineMarker(violations), extractMarkers(dependees));
	}

	private static MarkInternalDependencies determineMarker(Either<Exception, ImmutableSet<Violation>> violations) {
		return violations.fold(
				exception -> MarkInternalDependencies.UNKNOWN,
				viols -> viols.isEmpty() ? MarkInternalDependencies.NONE : MarkInternalDependencies.DIRECT);
	}

	private static Optional<Stream<MarkTransitiveInternalDependencies>> extractMarkers(
			Either<Exception, ImmutableSet<CompletedArtifact>> dependees) {
		return dependees
				.map(deps -> deps.stream().map(CompletedArtifact::transitiveMarker))
				.fold(exception -> Optional.empty(), Optional::of);
	}

	public static CompletedArtifactBuilder forArtifact(ArtifactCoordinates artifact) {
		return new CompletedArtifactBuilder(artifact);
	}

	// GETTER

	@Override
	public ArtifactCoordinates coordinates() {
		return artifact;
	}

	public MarkTransitiveInternalDependencies transitiveMarker() {
		return transitiveMarker;
	}

	public Either<Exception, ImmutableSet<Violation>> violations() {
		return violations;
	}

	public Either<Exception, ImmutableSet<CompletedArtifact>> dependees() {
		return dependees;
	}

	// EQUALS, HASHCODE, STRING

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CompletedArtifact that = (CompletedArtifact) o;
		return Objects.equals(artifact, that.artifact)
				&& Objects.equals(dependees, that.dependees);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artifact, dependees);
	}

	@Override
	public String toString() {
		return artifact + " (completed)";
	}

	public String toLongString() {
		return toLongString("\t");
	}

	public String toLongString(String indent) {
		return indent + artifact.toString() + "\n"
				+ indent + indent + "violations: " + violationsLongString() + "\n"
				+ indent + indent + "dependees: " + dependeesAsString();
	}

	private String violationsLongString() {
		if (violations.isLeft())
			return violations().getLeft().getMessage();
		if (violations.get().isEmpty())
			return "none";
		return violations.get().stream()
				.map(Violation::toString)
				.collect(joining(", "));
	}

	private String dependeesAsString() {
		if (dependees.isLeft())
			return dependees().getLeft().getMessage();
		if (dependees.isEmpty())
			return "none";
		return dependees.get().stream()
				.map(CompletedArtifact::coordinates)
				.map(ArtifactCoordinates::toString)
				.collect(joining(", "));
	}

	// BUILDER

	public static class CompletedArtifactBuilder {

		private final ArtifactCoordinates artifact;

		private Either<Exception, ImmutableSet<Violation>> violations;
		private Either<Exception, ImmutableSet<CompletedArtifact>> dependees;

		private CompletedArtifactBuilder(ArtifactCoordinates artifact) {
			this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.");
		}

		public CompletedArtifactBuilder withViolations(ImmutableSet<Violation> violations) {
			if (this.violations != null)
				throw new IllegalStateException("Violations (or an analysis error) were already specified.");
			this.violations = Either.right(requireNonNull(violations, "The argument 'violations' must not be null."));
			return this;
		}

		public CompletedArtifactBuilder violationAnalysisFailedWith(Exception violationAnalysisException) {
			if (this.violations != null)
				throw new IllegalStateException("Violations (or an analysis error) were already specified.");
			this.violations = Either.left(requireNonNull(
					violationAnalysisException,
					"The argument 'violationAnalysisException' must not be null."));
			return this;
		}

		public CompletedArtifactBuilder withDependees(ImmutableSet<CompletedArtifact> dependees) {
			if (this.dependees != null)
				throw new IllegalStateException("Dependees (or a resolution error) were already specified.");
			this.dependees = Either.right(requireNonNull(dependees, "The argument 'dependees' must not be null."));
			return this;
		}

		public CompletedArtifactBuilder dependeeResolutionFailedWith(Exception dependeeResolutionException) {
			if (this.dependees != null)
				throw new IllegalStateException("Dependees (or a resolution error) were already specified.");
			this.dependees = Either.left(requireNonNull(
					dependeeResolutionException,
					"The argument 'dependeeResolutionException' must not be null."));
			return this;
		}

		public CompletedArtifact build() {
			if (violations == null)
				throw new IllegalStateException("Neither violations nor an analysis error were specified.");
			if (dependees == null)
				throw new IllegalStateException("Neither dependees nor a resolution error were specified.");
			return new CompletedArtifact(artifact, violations, dependees);
		}
	}

}
