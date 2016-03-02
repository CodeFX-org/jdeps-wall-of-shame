package org.codefx.jwos.file.persistence;

import org.codefx.jwos.artifact.DownloadedArtifact;

import java.nio.file.Paths;

public class PersistentDownloadedArtifact {

	public PersistentArtifactCoordinates artifact;
	public String path;

	public static PersistentDownloadedArtifact from(DownloadedArtifact artifact) {
		PersistentDownloadedArtifact persistent = new PersistentDownloadedArtifact();
		persistent.artifact = PersistentArtifactCoordinates.from(artifact.coordinates());
		persistent.path = artifact.path().toString();
		return persistent;
	}

	public DownloadedArtifact toArtifact() {
		return new DownloadedArtifact(
				artifact.toArtifact(),
				Paths.get(path)
		);
	}

}
