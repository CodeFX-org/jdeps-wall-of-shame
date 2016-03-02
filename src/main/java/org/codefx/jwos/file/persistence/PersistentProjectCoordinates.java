package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.artifact.ProjectCoordinates;

import static java.lang.String.format;

public class PersistentProjectCoordinates {

	public String coordinates;

	public static PersistentProjectCoordinates from(ProjectCoordinates project) {
		PersistentProjectCoordinates persistent = new PersistentProjectCoordinates();
		persistent.coordinates = project.groupId() + ":" + project.artifactId();
		return persistent;
	}

	public ProjectCoordinates toProject() {
		String[] coords = coordinates.split(":");
		if (coords.length != 2)
			throw new IllegalArgumentException(format("Invalid project coordinates: \"%s\"", coordinates));
		return ProjectCoordinates.from(coords[0], coords[1]);
	}

}
