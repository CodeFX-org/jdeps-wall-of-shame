package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import org.codefx.jwos.artifact.ResolvedProject;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.codefx.jwos.Util.toImmutableSet;

public class PersistentResolvedProject {

	public PersistentProjectCoordinates project;
	public Set<PersistentArtifactCoordinates> versions;

	public static PersistentResolvedProject from(ResolvedProject project) {
		PersistentResolvedProject persistent = new PersistentResolvedProject();
		persistent.project = PersistentProjectCoordinates.from(project.coordinates());
		persistent.versions = project
				.versions().stream()
				.map(PersistentArtifactCoordinates::from)
				.collect(toSet());
		return persistent;
	}

	public ResolvedProject toProject() {
		return new ResolvedProject(
				project.toProject(),
				versions.stream()
						.map(PersistentArtifactCoordinates::toArtifact)
						.collect(toImmutableSet())
		);
	}


}
