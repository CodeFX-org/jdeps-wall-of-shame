package org.codefx.jwos.artifact;

import com.google.common.collect.ImmutableSet;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class ResolvedArtifact implements IdentifiesArtifact {

	private final ArtifactCoordinates artifact;
	private final Path path;
	private final ImmutableSet<ArtifactCoordinates> dependencies;

	public ResolvedArtifact(ArtifactCoordinates artifact, Path path, ImmutableSet<ArtifactCoordinates> dependencies) {
		this.artifact = requireNonNull(artifact, "The argument 'artifact' must not be null.");
		this.path = requireNonNull(path, "The argument 'path' must not be null.");
		this.dependencies = requireNonNull(dependencies, "The argument 'dependencies' must not be null.");
	}

	@Override
	public ArtifactCoordinates artifact() {
		return artifact;
	}

	public Path path() {
		return path;
	}

	public ImmutableSet<ArtifactCoordinates> dependencies() {
		return dependencies;
	}

	@Override
	public String toString() {
		return artifact + " (resolved)";
	}

}
