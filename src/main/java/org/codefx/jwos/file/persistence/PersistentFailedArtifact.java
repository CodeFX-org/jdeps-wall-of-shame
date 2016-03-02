package org.codefx.jwos.file.persistence;

import org.codefx.jwos.artifact.FailedArtifact;

public class PersistentFailedArtifact {

	public PersistentArtifactCoordinates artifact;
	public String errorMessage;

	public static PersistentFailedArtifact from(FailedArtifact artifact) {
		PersistentFailedArtifact persistent = new PersistentFailedArtifact();
		persistent.artifact = PersistentArtifactCoordinates.from(artifact.coordinates());
		persistent.errorMessage = artifact.error().getMessage();
		return persistent;
	}

	public FailedArtifact toArtifact() {
		return new FailedArtifact(
				artifact.toArtifact(),
				new Exception(errorMessage)
		);
	}


}
