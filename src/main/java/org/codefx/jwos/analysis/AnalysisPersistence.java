package org.codefx.jwos.analysis;// NOT_PUBLISHED

import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;

import java.util.Collection;

/**
 * Stores the results and failures of an analysis and makes results from former and the current runs available.
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

	void addAnalyzedArtifact(AnalyzedArtifact artifact);

	void addAnalysisFailure(FailedArtifact artifact);

	void addResolvedArtifact(ResolvedArtifact artifact);

	void addArtifactResolutionFailure(FailedArtifact artifact);

	void addResult(DeeplyAnalyzedArtifact deeplyAnalyzedArtifact);

}
