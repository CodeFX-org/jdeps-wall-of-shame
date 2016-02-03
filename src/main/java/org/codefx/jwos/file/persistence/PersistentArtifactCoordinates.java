package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.artifact.ArtifactCoordinates;

public class PersistentArtifactCoordinates {

	public String groupId;
	public String artifactId;
	public String version;

	public static PersistentArtifactCoordinates from(ArtifactCoordinates artifact) {
		PersistentArtifactCoordinates persistent = new PersistentArtifactCoordinates();
		persistent.groupId = artifact.groupId();
		persistent.artifactId = artifact.artifactId();
		persistent.version = artifact.version();
		return persistent;
	}

	public ArtifactCoordinates toArtifact() {
		return ArtifactCoordinates.from(groupId, artifactId, version);
	}

}
