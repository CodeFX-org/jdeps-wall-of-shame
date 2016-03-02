package org.codefx.jwos.analysis;

import org.codefx.jwos.analysis.channel.TaskChannel;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;

/**
 * A collection of all channels used by the analysis.
 */
public interface AnalysisTaskChannels {
	
	TaskChannel<Void, ProjectCoordinates, Exception> addProjects();

	TaskChannel<ProjectCoordinates, ResolvedProject, FailedProject> resolveVersions();

	TaskChannel<ArtifactCoordinates, DownloadedArtifact, FailedArtifact> downloadArtifacts();

	TaskChannel<DownloadedArtifact, AnalyzedArtifact, FailedArtifact> analyzeArtifacts();

	TaskChannel<ArtifactCoordinates, ResolvedArtifact, FailedArtifact> resolveDependencies();

	TaskChannel<DeeplyAnalyzedArtifact, Void, Void> outputResults();
}
