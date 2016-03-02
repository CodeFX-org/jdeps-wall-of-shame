package org.codefx.jwos.file.persistence;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedProject;

import java.util.List;

import static org.codefx.jwos.Util.transformToList;

public class PersistentResolvedProject {

	public PersistentProjectCoordinates project;
	public List<String> versions;

	public static PersistentResolvedProject from(ResolvedProject project) {
		PersistentResolvedProject persistent = new PersistentResolvedProject();
		persistent.project = PersistentProjectCoordinates.from(project.coordinates());
		persistent.versions = transformToList(project.versions(), ArtifactCoordinates::version);
		return persistent;
	}

	public ResolvedProject toProject() {
		ProjectCoordinates projectCoordinates = project.toProject();
		ImmutableSet<ArtifactCoordinates> artifacts = projectCoordinates.toArtifactsWithVersions(versions.stream());
		return new ResolvedProject(projectCoordinates,artifacts);
	}

}
