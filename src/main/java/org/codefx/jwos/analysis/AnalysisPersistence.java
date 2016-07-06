package org.codefx.jwos.analysis;

import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.CompletedArtifact;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;

import java.util.Collection;

/**
 * Stores the results and errors of an analysis and makes results from former and the current runs available.
 * <p>
 * Implementations must allow concurrent modifications.
 */
public interface AnalysisPersistence {

	// QUERY

	//  - projects

	Collection<ProjectCoordinates> projectsUnmodifiable();

	Collection<ResolvedProject> resolvedProjectsUnmodifiable();

	Collection<FailedProject> projectResolutionErrorsUnmodifiable();

	//  - artifacts

	Collection<DownloadedArtifact> downloadedArtifactsUnmodifiable();

	Collection<FailedArtifact> artifactDownloadErrorsUnmodifiable();

	Collection<AnalyzedArtifact> analyzedArtifactsUnmodifiable();

	Collection<FailedArtifact> artifactAnalysisErrorsUnmodifiable();

	Collection<ResolvedArtifact> resolvedArtifactsUnmodifiable();

	Collection<FailedArtifact> artifactResolutionErrorsUnmodifiable();

	// ADD

	//  - projects

	void addProject(ProjectCoordinates project);

	void addResolvedProject(ResolvedProject project);

	void addProjectResolutionError(FailedProject project);

	//  - artifacts

	void addDownloadedArtifact(DownloadedArtifact artifact);

	void addDownloadError(FailedArtifact artifact);

	void addAnalyzedArtifact(AnalyzedArtifact artifact);

	void addAnalysisError(FailedArtifact artifact);

	void addResolvedArtifact(ResolvedArtifact artifact);

	void addArtifactResolutionError(FailedArtifact artifact);

	void addResult(CompletedArtifact completedArtifact);

}
