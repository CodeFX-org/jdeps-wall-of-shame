package org.codefx.jwos.artifact;

import com.google.common.collect.ImmutableSet;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * An artifact whose dependencies are resolved.
 */
public final class ResolvedArtifact implements IdentifiesArtifactTask<ImmutableSet<ArtifactCoordinates>> {

	private final ArtifactCoordinates artifact;
	private final ImmutableSet<ArtifactCoordinates> dependees;

	public ResolvedArtifact(ArtifactCoordinates artifact, ImmutableSet<ArtifactCoordinates> dependees) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.");
		this.dependees = requireNonNull(dependees, "The argument 'dependees' must not be null.");
	}

	@Override
	public ArtifactCoordinates coordinates() {
		return artifact;
	}

	@Override
	public ImmutableSet<ArtifactCoordinates> result() {
		return dependees;
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
		ResolvedArtifact that = (ResolvedArtifact) o;
		return Objects.equals(artifact, that.artifact)
				&& Objects.equals(dependees, that.dependees);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artifact, dependees);
	}

	@Override
	public String toString() {
		return artifact + " (resolved)";
	}

}
