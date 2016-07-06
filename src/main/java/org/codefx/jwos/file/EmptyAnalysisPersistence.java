package org.codefx.jwos.file;

import org.codefx.jwos.analysis.AnalysisPersistence;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.CompletedArtifact;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;

import java.util.Collection;
import java.util.Collections;

/**
 * An {@link AnalysisPersistence} that is empty and immutable.
 * <p>
 * The mutating methods can be called but do nothing.
 */
public class EmptyAnalysisPersistence implements AnalysisPersistence {

	@Override
	public Collection<ProjectCoordinates> projectsUnmodifiable() {
		return Collections.emptySet();
	}

	@Override
	public Collection<ResolvedProject> resolvedProjectsUnmodifiable() {
		return Collections.emptySet();
	}

	@Override
	public Collection<FailedProject> projectResolutionErrorsUnmodifiable() {
		return Collections.emptySet();
	}

	@Override
	public Collection<DownloadedArtifact> downloadedArtifactsUnmodifiable() {
		return Collections.emptySet();
	}

	@Override
	public Collection<FailedArtifact> artifactDownloadErrorsUnmodifiable() {
		return Collections.emptySet();
	}

	@Override
	public Collection<AnalyzedArtifact> analyzedArtifactsUnmodifiable() {
		return Collections.emptySet();
	}

	@Override
	public Collection<FailedArtifact> artifactAnalysisErrorsUnmodifiable() {
		return Collections.emptySet();
	}

	@Override
	public Collection<ResolvedArtifact> resolvedArtifactsUnmodifiable() {
		return Collections.emptySet();
	}

	@Override
	public Collection<FailedArtifact> artifactResolutionErrorsUnmodifiable() {
		return Collections.emptySet();
	}

	@Override
	public void addProject(ProjectCoordinates project) {
		// do nothing
	}

	@Override
	public void addResolvedProject(ResolvedProject project) {
		// do nothing
	}

	@Override
	public void addProjectResolutionError(FailedProject project) {
		// do nothing
	}

	@Override
	public void addDownloadedArtifact(DownloadedArtifact artifact) {
		// do nothing
	}

	@Override
	public void addDownloadError(FailedArtifact artifact) {
		// do nothing
	}

	@Override
	public void addAnalyzedArtifact(AnalyzedArtifact artifact) {
		// do nothing
	}

	@Override
	public void addAnalysisError(FailedArtifact artifact) {
		// do nothing
	}

	@Override
	public void addResolvedArtifact(ResolvedArtifact artifact) {
		// do nothing
	}

	@Override
	public void addArtifactResolutionError(FailedArtifact artifact) {
		// do nothing
	}

	@Override
	public void addResult(CompletedArtifact completedArtifact) {
		// do nothing
	}
}
