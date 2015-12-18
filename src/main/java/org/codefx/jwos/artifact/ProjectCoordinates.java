package org.codefx.jwos.artifact;

import com.google.common.collect.ImmutableList;
import org.eclipse.aether.version.Version;

import java.util.Collection;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public final class ProjectCoordinates {

	private final String groupId;
	private final String artifactId;

	private ProjectCoordinates(String groupId, String artifactId) {
		this.groupId = requireNonNull(groupId, "The argument 'groupId' must not be null.");
		this.artifactId = requireNonNull(artifactId, "The argument 'artifactId' must not be null.");
	}

	public static ProjectCoordinates from(String groupId, String artifactId) {
		return new ProjectCoordinates(groupId, artifactId);
	}

	public ImmutableList<ArtifactCoordinates> toArtifactsWithVersions(Collection<Version> versions) {
		return versions.stream()
				.map(version -> ArtifactCoordinates.from(groupId, artifactId, version.toString()))
				.collect(collectingAndThen(toList(), ImmutableList::copyOf));
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
