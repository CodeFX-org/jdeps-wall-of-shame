package org.codefx.jwos.analysis;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.analysis.task.Task;
import org.codefx.jwos.artifact.IdentifiesProject;
import org.codefx.jwos.artifact.ProjectCoordinates;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

class ProjectNode implements IdentifiesProject {

	private final ProjectCoordinates project;

	/*
	 * The fields 'versions' and 'resolutionOfVersions' might seem redundant.
	 *
	 * The former holds the versions that came up at run time by scanning artifact dependencies.
	 * A project that is discovered at run time will initially only have one and later maybe more such artifacts.
	 * The latter holds the versions as reported by Maven.
	 *
	 * If other artifacts do not depend on versions that are not reported by Maven, both sets will eventually contain
	 * the same elements.
	 */

	private final Set<ArtifactNode> versions;
	private final Task<ImmutableSet<ArtifactNode>> resolutionOfVersions;

	public ProjectNode(IdentifiesProject project) {
		this.project = requireNonNull(project, "The argument 'project' must not be null.").coordinates();
		this.versions = new HashSet<>();

		resolutionOfVersions = new Task<>();
	}

	@Override
	public ProjectCoordinates coordinates() {
		return project;
	}

	public Set<ArtifactNode> versions() {
		return versions;
	}

	public Task<ImmutableSet<ArtifactNode>> resolution() {
		return resolutionOfVersions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ProjectNode that = (ProjectNode) o;
		return Objects.equals(project, that.project);
	}

	@Override
	public int hashCode() {
		return Objects.hash(project);
	}

	@Override
	public String toString() {
		return "Node: " + project;
	}

}
