package org.codefx.jwos.artifact;

import com.google.common.collect.ImmutableSet;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A project whose versions are resolved.
 */
public final class ResolvedProject implements IdentifiesProjectTask<ImmutableSet<ArtifactCoordinates>> {

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

	@Override
	public ImmutableSet<ArtifactCoordinates> result() {
		return versions;
	}

	public ImmutableSet<ArtifactCoordinates> versions() {
		return versions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ResolvedProject that = (ResolvedProject) o;
		return Objects.equals(project, that.project)
				&& Objects.equals(versions, that.versions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(project, versions);
	}

	@Override
	public String toString() {
		return project + " (resolved)";
	}

}
