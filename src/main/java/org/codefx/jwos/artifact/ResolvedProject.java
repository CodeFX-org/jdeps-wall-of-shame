package org.codefx.jwos.artifact;

import com.google.common.collect.ImmutableSet;

import static java.util.Objects.requireNonNull;

/**
 * A project whose versions are resolved.
 */
public final class ResolvedProject implements IdentifiesProject {

	private final ProjectCoordinates project;
	private final ImmutableSet<ArtifactCoordinates> versions;

	public ResolvedProject(ProjectCoordinates project, ImmutableSet<ArtifactCoordinates> versions) {
		this.project = requireNonNull(project, "The argument 'project' must not be null.");
		this.versions = requireNonNull(versions, "The argument 'versions' must not be null.");
	}

	@Override
	public ProjectCoordinates coordinates() {
		return project;
	}

	public ImmutableSet<ArtifactCoordinates> versions() {
		return versions;
	}

	@Override
	public String toString() {
		return project + " (resolved)";
	}

}
