package org.codefx.jwos;

import org.codefx.jwos.analysis.AnalysisTaskManager;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.CompletedArtifact;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;
import org.codefx.jwos.computation.Computation;
import org.codefx.jwos.computation.ComputationThread;
import org.codefx.jwos.computation.RecurrentComputation;
import org.codefx.jwos.computation.SendError;
import org.codefx.jwos.computation.SendResult;
import org.codefx.jwos.computation.TaskComputation;
import org.codefx.jwos.computation.TaskSink;
import org.codefx.jwos.computation.TaskSource;
import org.codefx.jwos.discovery.ProjectListFile;
import org.codefx.jwos.file.WallFiles;
import org.codefx.jwos.file.WallOfShame;
import org.codefx.jwos.file.YamlAnalysisPersistence;
import org.codefx.jwos.git.GitInformation;
import org.codefx.jwos.jdeps.JDeps;
import org.codefx.jwos.maven.MavenCentral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.singleton;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;

/**
 * Puts the pieces together:
 * <ul>
 *     <li>projects are read from a {@link ProjectListFile}
 *     <li>project versions are identified with {@link MavenCentral} and used to create artifact coordinates
 *     <li>artifacts are downloaded (with {@code MavenCentral}) and analysed (with {@link JDeps})
 *     <li>an artifact's dependees (the artifacts on which it depends) are resolved (with {@code MavenCentral})
 *     so they (and all other versions of the same project) can be analysed as well
 *     <li>results are written to the {@link WallOfShame}
 * </ul>
 * All of these steps are called tasks and centrally managed by the {@link AnalysisTaskManager}. All that is needed
 * here is to get the tasks, hand them to the classes mentioned above, and send results and errors back to the task
 * manager.
 */
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger("Main");

	public static void main(String[] args) throws IOException {
		LOGGER.info("Processing existing results...");
		Path resultFile = Util.getPathToExistingResourceFile(Util.RESULT_FILE_NAME);
		YamlAnalysisPersistence persistence = createYamlPersistence(resultFile);

		LOGGER.info("Setting up task manager...");
		AnalysisTaskManager taskManager = new AnalysisTaskManager(persistence);

		LOGGER.info("Setting up tasks...");
		MavenCentral maven = new MavenCentral(Util.LOCAL_MAVEN_REPOSITORY.toString());
		JDeps jdeps = new JDeps();
		WallOfShame wallOfShame = WallOfShame.openExistingDirectory(
				WallFiles.defaultsInDirectory(Util.PAGES_DIRECTORY),
				GitInformation.simple(
						Util.GIT_REPOSITORY_URL,
						Util.PAGES_DIRECTORY,
						Util.GIT_USER_NAME,
						Util.GIT_PASSWORD,
						Util.GIT_EMAIL));

		Stream<ComputationThread> threads = Stream
				.of(
						createComputationsToReadProjectFiles(taskManager),
						createComputationsTo(resolveProjectVersions(taskManager, maven), 1),
						createComputationsTo(downloadArtifact(taskManager, maven), 2),
						createComputationsTo(analyzeArtifact(taskManager, jdeps), 2),
						createComputationsTo(resolveArtifactDependees(taskManager, maven), 3),
						createComputationsTo(outputResults(taskManager, wallOfShame), 1),
						createComputationsTo(writeToYaml(persistence, resultFile), 1))
				.flatMap(identity())
				.map(ComputationThread::new);

		LOGGER.info("Starting computation...");
		threads.forEach(Thread::start);
		new Thread(taskManager::manageQueues, "Manage Queue").run();
	}

	private static YamlAnalysisPersistence createYamlPersistence(Path resultFile) throws IOException {
		String yamlString = Files.lines(resultFile).collect(joining("\n"));
		return YamlAnalysisPersistence.fromString(yamlString);
	}

	private static Stream<Computation> createComputationsToReadProjectFiles(AnalysisTaskManager taskManager) {
		return Arrays
				.stream(Util.PROJECT_LIST_FILE_NAMES)
				.map(fileName -> createComputationToReadProjectFile(fileName, taskManager))
				.filter(Optional::isPresent)
				.map(Optional::get);
	}

	private static Optional<Computation> createComputationToReadProjectFile(
			String fileName,
			AnalysisTaskManager taskManager) {
		try {
			Path file = Util.getPathToExistingResourceFile(fileName);
			ProjectListFile listFile = new ProjectListFile(file).open();
			Computation computation = new TaskSource<>(
					"Read Project File",
					ignored -> listFile.readNextProject(),
					taskManager::addProject,
					(ignored, error) -> taskManager.findingProjectFailed(error));
			return Optional.of(computation);
		} catch (IOException ex) {
			LOGGER.error("Failed to read project file '" + fileName + "'.", ex);
			return Optional.empty();
		}
	}

	private static Stream<Computation> createComputationsTo(Computation computation, int threads) {
		return IntStream
				.range(0, threads)
				.mapToObj(any -> computation);
	}

	private static TaskComputation<ProjectCoordinates, ResolvedProject> resolveProjectVersions(
			AnalysisTaskManager taskManager, MavenCentral maven) {
		return new TaskComputation<>(
				"Resolve Project Versions",
				taskManager::getNextToResolveVersions,
				maven::detectAllVersionsOf,
				taskManager::resolvedVersions,
				sendProjectError(taskManager::versionResolutionFailed));
	}

	private static TaskComputation<ArtifactCoordinates, DownloadedArtifact> downloadArtifact(
			AnalysisTaskManager taskManager, MavenCentral maven) {
		return new TaskComputation<>(
				"Download Artifact",
				taskManager::getNextToDownload,
				maven::downloadArtifact,
				taskManager::downloaded,
				sendArtifactError(taskManager::downloadFailed));
	}

	private static TaskComputation<DownloadedArtifact, AnalyzedArtifact> analyzeArtifact(
			AnalysisTaskManager taskManager, JDeps jdeps) {
		return new TaskComputation<>(
				"Analyze Artifact",
				taskManager::getNextToAnalyze,
				jdeps::analyze,
				taskManager::analyzed,
				(DownloadedArtifact artifact, Exception error)
						-> taskManager.analysisFailed(new FailedArtifact(artifact.coordinates(), error)));
	}

	private static TaskComputation<ArtifactCoordinates, ResolvedArtifact> resolveArtifactDependees(
			AnalysisTaskManager taskManager, MavenCentral maven) {
		return new TaskComputation<>(
				"Resolve Artifact Dependencies",
				taskManager::getNextToResolveDependencies,
				maven::resolveArtifact,
				taskManager::resolvedDependencies,
				sendArtifactError(taskManager::dependencyResolutionFailed));
	}

	private static Computation writeToYaml(
			YamlAnalysisPersistence persistence,
			Path resultFile) {
		return new RecurrentComputation(
				"Write Results To YAML",
				() -> {
					String yamlString = persistence.toYaml();
					Files.write(resultFile, singleton(yamlString));
				},
				200);
	}

	private static TaskSink<CompletedArtifact> outputResults(AnalysisTaskManager taskManager, WallOfShame wallOfShame) {
		return new TaskSink<>(
				"Output Results",
				taskManager::getNextToOutput,
				artifact -> {
					wallOfShame.addArtifacts(artifact);
					wallOfShame.write();
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
