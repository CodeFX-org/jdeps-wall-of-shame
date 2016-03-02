package org.codefx.jwos.file.persistence;

import org.codefx.jwos.artifact.ArtifactCoordinates;

import static java.lang.String.format;

public class PersistentArtifactCoordinates {

	public String coordinates;

	public static PersistentArtifactCoordinates from(ArtifactCoordinates artifact) {
		PersistentArtifactCoordinates persistent = new PersistentArtifactCoordinates();
		persistent.coordinates = artifact.groupId() + ":" + artifact.artifactId() + ":"+ artifact.version();
		return persistent;
	}

	public ArtifactCoordinates toArtifact() {
		String[] coords = coordinates.split(":");
		if (coords.length != 3)
			throw new IllegalArgumentException(format("Invalid artifact coordinates: \"%s\"", coordinates));
		return ArtifactCoordinates.from(coords[0], coords[1], coords[2]);
	}

}
