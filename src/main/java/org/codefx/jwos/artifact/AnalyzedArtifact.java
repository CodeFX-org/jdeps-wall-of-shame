package org.codefx.jwos.artifact;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.jdeps.dependency.Violation;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class AnalyzedArtifact implements IdentifiesArtifact {

	private final ArtifactCoordinates artifact;
	private final ImmutableSet<Violation> violations;
	private final ImmutableSet<ArtifactCoordinates> dependees;

	public AnalyzedArtifact(
			ArtifactCoordinates artifact,
			ImmutableSet<Violation> violations,
			ImmutableSet<ArtifactCoordinates> dependees) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.");
		this.violations = requireNonNull(violations, "The argument 'violations' must not be null.");
		this.dependees = requireNonNull(dependees, "The argument 'dependees' must not be null.");
	}

	@Override
	public ArtifactCoordinates artifact() {
		return artifact;
	}

	public ImmutableSet<Violation> violations() {
		return violations;
	}

	public ImmutableSet<ArtifactCoordinates> dependees() {
		return dependees;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AnalyzedArtifact that = (AnalyzedArtifact) o;
		return Objects.equals(artifact, that.artifact);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artifact);
	}

	@Override
	public String toString() {
		return artifact.toString() + " (analyzed)";
	}
}
