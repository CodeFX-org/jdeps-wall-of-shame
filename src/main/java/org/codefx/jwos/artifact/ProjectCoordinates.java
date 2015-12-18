package org.codefx.jwos.artifact;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

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
