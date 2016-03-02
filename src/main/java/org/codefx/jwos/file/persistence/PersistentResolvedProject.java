package org.codefx.jwos.file.persistence;// NOT_PUBLISHED

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedProject;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class PersistentResolvedProject {

	public PersistentProjectCoordinates project;
	public List<String> versions;

	public static PersistentResolvedProject from(ResolvedProject project) {
		PersistentResolvedProject persistent = new PersistentResolvedProject();
		persistent.project = PersistentProjectCoordinates.from(project.coordinates());
		persistent.versions = project
				.versions().stream()
				.map(ArtifactCoordinates::version)
				.collect(toList());
		return persistent;
	}

	public ResolvedProject toProject() {
		ProjectCoordinates projectCoordinates = project.toProject();
		ImmutableSet<ArtifactCoordinates> artifacts = projectCoordinates.toArtifactsWithVersions(versions.stream());
		return new ResolvedProject(projectCoordinates,artifacts);
	}

}
