package org.codefx.jwos;

import org.codefx.jwos.analysis.AnalysisTaskManager;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;
import org.codefx.jwos.computation.Computation;
import org.codefx.jwos.computation.ComputationRunnable;
import org.codefx.jwos.computation.SendError;
import org.codefx.jwos.computation.SendResult;
import org.codefx.jwos.computation.TaskComputation;
import org.codefx.jwos.computation.TaskSink;
import org.codefx.jwos.computation.TaskSource;
import org.codefx.jwos.discovery.ProjectListFile;
import org.codefx.jwos.file.ResultFile;
import org.codefx.jwos.jdeps.JDeps;
import org.codefx.jwos.maven.MavenCentral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger("Main");

	public static void main(String[] args) throws IOException {
		ResultFile resultFile = ResultFile.read(Util.getPathToResourceFile(Util.RESULT_FILE_NAME));

		AnalysisTaskManager taskManager = new AnalysisTaskManager();
		MavenCentral maven = new MavenCentral();
		JDeps jdeps = new JDeps();

		Stream
				.of(
						createComputationsToReadProjectFiles(taskManager),
						createComputationsTo(resolveProjectVersions(taskManager, maven), 1),
						createComputationsTo(downloadArtifact(taskManager, maven), 2),
						createComputationsTo(analyzeArtifact(taskManager, jdeps), 2),
						createComputationsTo(resolveArtifactDependees(taskManager, maven), 3),
						createComputationsTo(outputResults(taskManager, resultFile), 1))
				.flatMap(identity())
				.map(ComputationRunnable::new)
				.map(Thread::new)
				.forEach(Thread::start);
		new Thread(taskManager::manageQueues).run();
	}

	private static Stream<Computation> createComputationsToReadProjectFiles(AnalysisTaskManager taskManager) {
		return Arrays
				.stream(Util.PROJECT_LIST_FILE_NAMES)
				.map(fileName -> createComputationToReadProjectFile(fileName, taskManager))
				.filter(Optional::isPresent)
				.map(Optional::get);
	}

	private static Stream<Computation> createComputationsTo(Computation computation, int threads) {
		return IntStream
				.range(0, threads)
				.mapToObj(any -> computation);
	}

	private static Optional<Computation> createComputationToReadProjectFile(
			String fileName,
			AnalysisTaskManager taskManager) {
		try {
			Path file = Util.getPathToResourceFile(fileName);
			ProjectListFile listFile = new ProjectListFile(file).open();
			Computation computation = new TaskSource<>(
					ignored -> listFile.readNextProject(),
					taskManager::addProject,
					(ignored, error) -> taskManager.findingProjectFailed(error));
			return Optional.of(computation);
		} catch (IOException ex) {
			LOGGER.error("Failed to read project file '" + fileName + "'.", ex);
			return Optional.empty();
		}
	}

	private static TaskComputation<ProjectCoordinates, ResolvedProject> resolveProjectVersions(
			AnalysisTaskManager taskManager, MavenCentral maven) {
		return new TaskComputation<>(
				taskManager::getNextToResolveVersions,
				maven::detectAllVersionsOf,
				taskManager::resolvedVersions,
				sendProjectError(taskManager::versionResolutionFailed));
	}

	private static TaskComputation<ArtifactCoordinates, DownloadedArtifact> downloadArtifact(
			AnalysisTaskManager taskManager, MavenCentral maven) {
		return new TaskComputation<>(
				taskManager::getNextToDownload,
				maven::downloadArtifact,
				taskManager::downloaded,
				sendArtifactError(taskManager::downloadFailed));
	}

	private static TaskComputation<DownloadedArtifact, AnalyzedArtifact> analyzeArtifact(
			AnalysisTaskManager taskManager, JDeps jdeps) {
		return new TaskComputation<>(
				taskManager::getNextToAnalyze,
				jdeps::analyze,
				taskManager::analyzed,
				(DownloadedArtifact artifact, Exception error)
						-> taskManager.analysisFailed(new FailedArtifact(artifact.coordinates(), error)));
	}

	private static TaskComputation<ArtifactCoordinates, ResolvedArtifact> resolveArtifactDependees(
			AnalysisTaskManager taskManager, MavenCentral maven) {
		return new TaskComputation<>(
				taskManager::getNextToResolveDependencies,
				maven::resolveArtifact,
				taskManager::resolvedDependencies,
				sendArtifactError(taskManager::dependencyResolutionFailed));
	}

	private static TaskSink<DeeplyAnalyzedArtifact> outputResults(
			AnalysisTaskManager taskManager, ResultFile resultFile) {
		return new TaskSink<>(
				taskManager::getNextToOutput,
				artifact -> {
					resultFile.addArtifacts(artifact);
					resultFile.write();
					return null;
				},
				(artifact, error) -> LOGGER.error("Failed to write result '" + artifact.coordinates() + "'.", error));
	}

	private static SendError<ProjectCoordinates> sendProjectError(SendResult<FailedProject> sendFailedProject) {
		return (ProjectCoordinates project, Exception error) -> {
			FailedProject failedProject = new FailedProject(project, error);
			sendFailedProject.send(failedProject);
		};
	}

	private static SendError<ArtifactCoordinates> sendArtifactError(SendResult<FailedArtifact> sendFailedArtifact) {
		return (ArtifactCoordinates artifact, Exception error) -> {
			FailedArtifact failedArtifact = new FailedArtifact(artifact, error);
			sendFailedArtifact.send(failedArtifact);
		};
	}

}
