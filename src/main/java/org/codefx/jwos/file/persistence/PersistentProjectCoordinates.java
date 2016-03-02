package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.artifact.ProjectCoordinates;

public class PersistentProjectCoordinates {

	public String groupId;
	public String artifactId;

	public static PersistentProjectCoordinates from(ProjectCoordinates project) {
		PersistentProjectCoordinates persistent = new PersistentProjectCoordinates();
		persistent.groupId = project.groupId();
		persistent.artifactId = project.artifactId();
		return persistent;
	}

	public ProjectCoordinates toProject() {
		return ProjectCoordinates.from(groupId, artifactId);
	}

}
