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
 * A basic collection of task channels that do nothing fancy.
 */
class SimpleAnalysisTaskChannels implements AnalysisTaskChannels {

	private final TaskChannel<Void, ProjectCoordinates, Exception> addProjects;
	private final TaskChannel<ProjectCoordinates, ResolvedProject, FailedProject> resolveVersions;
	private final TaskChannel<ArtifactCoordinates, DownloadedArtifact, FailedArtifact> downloadArtifacts;
	private final TaskChannel<DownloadedArtifact, AnalyzedArtifact, FailedArtifact> analyzeArtifacts;
	private final TaskChannel<ArtifactCoordinates, ResolvedArtifact, FailedArtifact> resolveDependencies;
	private final TaskChannel<DeeplyAnalyzedArtifact, Void, Void> outputResults;

	public SimpleAnalysisTaskChannels() {
		addProjects = TaskChannel.namedAndUnbounded("add project");
		resolveVersions = TaskChannel.namedAndUnbounded("version resolution");
		downloadArtifacts = TaskChannel.namedAndUnbounded("download");
		analyzeArtifacts = TaskChannel.namedAndUnbounded("analysis");
		resolveDependencies = TaskChannel.namedAndUnbounded("dependency resolution");
		outputResults = TaskChannel.namedAndUnbounded("output");
	}

	@Override
	public TaskChannel<Void, ProjectCoordinates, Exception> addProjects() {
		return addProjects;
	}

	@Override
	public TaskChannel<ProjectCoordinates, ResolvedProject, FailedProject> resolveVersions() {
		return resolveVersions;
	}

	@Override
	public TaskChannel<ArtifactCoordinates, DownloadedArtifact, FailedArtifact> downloadArtifacts() {
		return downloadArtifacts;
	}

	@Override
	public TaskChannel<DownloadedArtifact, AnalyzedArtifact, FailedArtifact> analyzeArtifacts() {
		return analyzeArtifacts;
	}

	@Override
	public TaskChannel<ArtifactCoordinates, ResolvedArtifact, FailedArtifact> resolveDependencies() {
		return resolveDependencies;
	}

	@Override
	public TaskChannel<DeeplyAnalyzedArtifact, Void, Void> outputResults() {
		return outputResults;
	}
}
