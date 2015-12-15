package org.codefx.jwos.maven;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public final class ArtifactCoordinates implements IdentifiesArtifact {

	private final String groupId;
	private final String artifactId;
	private final String version;

	private ArtifactCoordinates(String groupId, String artifactId, String version) {
		this.groupId = requireNonNull(groupId, "The argument 'groupId' must not be null.");
		this.artifactId = requireNonNull(artifactId, "The argument 'artifactId' must not be null.");
		this.version = requireNonNull(version, "The argument 'version' must not be null.");
	}

	public static ArtifactCoordinates from(String groupId, String artifactId, String version) {
		return new ArtifactCoordinates(groupId, artifactId, version);
	}

	public static ArtifactCoordinates from(Artifact artifact) {
		return new ArtifactCoordinates(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
	}

	public Artifact toArtifact() {
		return new DefaultArtifact(groupId, artifactId, "jar", version);
	}

	public String groupId() {
		return groupId;
	}

	public String artifactId() {
		return artifactId;
	}

	public String version() {
		return version;
	}

	@Override
	public ArtifactCoordinates artifact() {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ArtifactCoordinates that = (ArtifactCoordinates) o;
		return Objects.equals(groupId, that.groupId) &&
				Objects.equals(artifactId, that.artifactId) &&
				Objects.equals(version, that.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, artifactId, version);
	}

	@Override
	public String toString() {
		return groupId + ":" + artifactId + ":" + version;
	}
}
