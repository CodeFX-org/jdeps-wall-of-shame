package org.codefx.jwos.file.persistence;

import org.codefx.jwos.artifact.FailedProject;

public class PersistentFailedProject {

	public PersistentProjectCoordinates artifact;
	public String errorMessage;

	public static PersistentFailedProject from(FailedProject artifact) {
		PersistentFailedProject persistent = new PersistentFailedProject();
		persistent.artifact = PersistentProjectCoordinates.from(artifact.coordinates());
		persistent.errorMessage = artifact.error().getMessage();
		return persistent;
	}

	public FailedProject toProject() {
		return new FailedProject(
				artifact.toProject(),
				new Exception(errorMessage)
		);
	}


}
