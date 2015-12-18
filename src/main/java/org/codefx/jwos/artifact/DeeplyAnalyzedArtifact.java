package org.codefx.jwos.artifact;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.jdeps.dependency.Violation;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class DeeplyAnalyzedArtifact implements IdentifiesArtifact {

	private final ArtifactCoordinates artifact;
	private final InternalDependencies marker;
	private final ImmutableSet<Violation> violations;
	private final ImmutableSet<DeeplyAnalyzedArtifact> dependees;

	public DeeplyAnalyzedArtifact(
			ArtifactCoordinates artifact,
			InternalDependencies marker,
			ImmutableSet<Violation> violations,
			ImmutableSet<DeeplyAnalyzedArtifact> dependees) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.");
		this.marker = requireNonNull(marker, "The argument 'marker' must not be null.");
		this.violations = requireNonNull(violations, "The argument 'violations' must not be null.");
		this.dependees = requireNonNull(dependees, "The argument 'dependees' must not be null.");
	}

	@Override
	public ArtifactCoordinates artifact() {
		return artifact;
	}

	public InternalDependencies marker() {
		return marker;
	}

	public ImmutableSet<Violation> violations() {
		return violations;
	}

	public ImmutableSet<DeeplyAnalyzedArtifact> dependees() {
		return dependees;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DeeplyAnalyzedArtifact that = (DeeplyAnalyzedArtifact) o;
		return Objects.equals(artifact, that.artifact);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artifact);
	}

	@Override
	public String toString() {
		return artifact.toString() + " (deeply analyzed)";
	}
}