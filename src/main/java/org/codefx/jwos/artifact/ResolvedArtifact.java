package org.codefx.jwos.artifact;

import com.google.common.collect.ImmutableSet;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * An artifact whose dependencies are resolved.
 */
public final class ResolvedArtifact implements IdentifiesArtifact {

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

	public ImmutableSet<ArtifactCoordinates> dependees() {
		return dependees;
	}

	@Override
	public String toString() {
		return artifact + " (resolved)";
	}

}
