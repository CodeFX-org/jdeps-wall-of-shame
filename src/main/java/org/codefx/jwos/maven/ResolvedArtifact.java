package org.codefx.jwos.maven;

import com.google.common.collect.ImmutableSet;

import java.nio.file.Path;

public class ResolvedArtifact {

	private final Path path;
	private final ImmutableSet<ArtifactCoordinates> dependencies;

	public ResolvedArtifact(Path path, ImmutableSet<ArtifactCoordinates> dependencies) {
		this.path = path;
		this.dependencies = dependencies;
	}

	public Path path() {
		return path;
	}

	public ImmutableSet<ArtifactCoordinates> dependencies() {
		return dependencies;
	}
}
