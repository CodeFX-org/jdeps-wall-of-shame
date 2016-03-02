package org.codefx.jwos.analysis;// NOT_PUBLISHED

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

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

/**
 * Creates regular and spying channels and connects them to a {@link AnalysisPersistence},
 * which was also used to replay results.
 */
class PersistenceAnalysisTaskChannels implements AnalysisTaskChannels {

	private final AnalysisPersistence persistence;
	
	private final TaskChannel<Void, ProjectCoordinates, Exception> addProjects;
	private final TaskChannel<ProjectCoordinates, ResolvedProject, FailedProject> resolveVersions;
	private final TaskChannel<ArtifactCoordinates, DownloadedArtifact, FailedArtifact> downloadArtifacts;
	private final TaskChannel<DownloadedArtifact, AnalyzedArtifact, FailedArtifact> analyzeArtifacts;
	private final TaskChannel<ArtifactCoordinates, ResolvedArtifact, FailedArtifact> resolveDependencies;
	private final TaskChannel<DeeplyAnalyzedArtifact, Void, Void> outputResults;

	private final TaskChannel<Void, ProjectCoordinates, Exception> addProjectsSpy;
	private final TaskChannel<ProjectCoordinates, ResolvedProject, FailedProject> resolveVersionsSpy;
	private final TaskChannel<DownloadedArtifact, AnalyzedArtifact, FailedArtifact> analyzeArtifactsSpy;
	private final TaskChannel<ArtifactCoordinates, ResolvedArtifact, FailedArtifact> resolveDependenciesSpy;
	private final TaskChannel<DeeplyAnalyzedArtifact, Void, Void> outputResultsSpy;

	public PersistenceAnalysisTaskChannels(AnalysisPersistence persistence) {
		this.persistence = requireNonNull(persistence, "The argument 'persistence' must not be null.");
		
		addProjectsSpy = TaskChannel.namedAndUnbounded("spying on add project");
		addProjects = TaskChannel
				.<Void, ProjectCoordinates, Exception>namedAndUnbounded("add project")
				.spy(addProjectsSpy)
				.replaying(persistence.projectsUnmodifiable(), emptySet());

		resolveVersionsSpy = TaskChannel.namedAndUnbounded("spying on version resolution");
		resolveVersions = TaskChannel
				.<ProjectCoordinates, ResolvedProject, FailedProject>namedAndUnbounded("version resolution")
				.spy(resolveVersionsSpy)
				.replaying(
						persistence.resolvedProjectsUnmodifiable(),
						persistence.projectResolutionErrorsUnmodifiable());

		// This task downloads JARs. Because it is not feasible to include them in another storing mechanism
		// (besides, e.g., the Maven repository) this program only deals with their paths. By themselves these are
		// worthless, though (files could have been deleted), so it makes no sense to store or replay them.
		downloadArtifacts = TaskChannel.namedAndUnbounded("download");

		analyzeArtifactsSpy = TaskChannel.namedAndUnbounded("spying on analysis");
		analyzeArtifacts = TaskChannel
				.<DownloadedArtifact, AnalyzedArtifact, FailedArtifact>namedAndUnbounded("analysis")
				.spy(analyzeArtifactsSpy)
				.replaying(
						persistence.analyzedArtifactsUnmodifiable(),
						persistence.artifactAnalysisErrorsUnmodifiable());

		resolveDependenciesSpy = TaskChannel.namedAndUnbounded("spying on dependency resolution");
		resolveDependencies = TaskChannel
				.<ArtifactCoordinates, ResolvedArtifact, FailedArtifact>namedAndUnbounded("dependency resolution")
				.spy(resolveDependenciesSpy)
				.replaying(
						persistence.resolvedArtifactsUnmodifiable(),
						persistence.artifactResolutionErrorsUnmodifiable());

		// This is only an output mechanism and there is no need to replay its results.
		// It is spied upon to enable gathering all results in one (code) location.
		outputResultsSpy = TaskChannel.namedAndUnbounded("spying on output");
		outputResults = TaskChannel
				.<DeeplyAnalyzedArtifact, Void, Void>namedAndUnbounded("output")
				.spy(outputResultsSpy);
	}
	
	/**
	 * Updates the {@link AnalysisPersistence} specified during construction with the messages that were sent
	 * on various channels since this method was last called.
	 */
	public void updatePersistence() {
		addProjectsSpy.drainResults().forEach(persistence::addProject);
		resolveVersionsSpy.drainResults().forEach(persistence::addResolvedProject);
		resolveVersionsSpy.drainErrors().forEach(persistence::addProjectResolutionError);

		analyzeArtifactsSpy.drainResults().forEach(persistence::addAnalyzedArtifact);
		analyzeArtifactsSpy.drainErrors().forEach(persistence::addAnalysisFailure);
		resolveDependenciesSpy.drainResults().forEach(persistence::addResolvedArtifact);
		resolveDependenciesSpy.drainErrors().forEach(persistence::addArtifactResolutionFailure);

		outputResultsSpy.drainTasks().forEach(persistence::addResult);
	}
	
	// IMPLEMENTATION OF 'AnalysisTaskChannels'

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
