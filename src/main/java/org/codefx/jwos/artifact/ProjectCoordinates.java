package org.codefx.jwos.artifact;

import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

/**
 * Uniquely identifies a project.
 */
public final class ProjectCoordinates implements IdentifiesProject {

	private final String groupId;
	private final String artifactId;

	private ProjectCoordinates(String groupId, String artifactId) {
		this.groupId = requireNonNull(groupId, "The argument 'groupId' must not be null.");
		this.artifactId = requireNonNull(artifactId, "The argument 'artifactId' must not be null.");
	}

	public static ProjectCoordinates from(String groupId, String artifactId) {
		return new ProjectCoordinates(groupId, artifactId);
	}

	public ImmutableSet<ArtifactCoordinates> toArtifactsWithVersions(Stream<String> versions) {
		return versions
				.map(version -> ArtifactCoordinates.from(groupId, artifactId, version))
				.collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
	}

	@Override
	public ProjectCoordinates coordinates() {
		return this;
	}

	public String groupId() {
		return groupId;
	}

	public String artifactId() {
		return artifactId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ProjectCoordinates that = (ProjectCoordinates) o;
		return Objects.equals(groupId, that.groupId) &&
				Objects.equals(artifactId, that.artifactId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, artifactId);
	}

	@Override
	public String toString() {
		return groupId + ":" + artifactId;
	}
}
