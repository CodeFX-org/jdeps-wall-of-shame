package org.codefx.jwos.artifact;

import com.google.common.collect.ImmutableSet;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class ResolvedArtifact implements IdentifiesArtifact {

	private final ArtifactCoordinates artifact;
	private final Path path;
	private final ImmutableSet<ArtifactCoordinates> dependees;

	public ResolvedArtifact(ArtifactCoordinates artifact, Path path, ImmutableSet<ArtifactCoordinates> dependees) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.");
		this.path = requireNonNull(path, "The argument 'path' must not be null.");
		this.dependees = requireNonNull(dependees, "The argument 'dependees' must not be null.");
	}

	@Override
	public ArtifactCoordinates artifact() {
		return artifact;
	}

	public Path path() {
		return path;
	}

	public ImmutableSet<ArtifactCoordinates> dependees() {
		return dependees;
	}

	@Override
	public String toString() {
		return artifact + " (resolved)";
	}

}
