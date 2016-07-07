package org.codefx.jwos;

import org.codefx.jwos.analysis.AnalysisTaskManager;
import org.codefx.jwos.artifact.CompletedArtifact;
import org.codefx.jwos.computation.ComputationThread;
import org.codefx.jwos.computation.TaskSink;
import org.codefx.jwos.file.WallFiles;
import org.codefx.jwos.file.WallOfShame;
import org.codefx.jwos.file.YamlAnalysisPersistence;
import org.codefx.jwos.git.GitInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Reformat {

	private static final Logger LOGGER = LoggerFactory.getLogger("Reformat");

	public static void main(String[] args) throws Exception {
		LOGGER.info("Processing existing results...");
		Path resultFile = Util.getPathToExistingResourceFile(Util.RESULT_FILE_NAME);
		YamlAnalysisPersistence persistence = Util.createYamlPersistence(resultFile);

		LOGGER.info("Setting up task manager...");
		AnalysisTaskManager taskManager = new AnalysisTaskManager(persistence);

		LOGGER.info("Setting output task...");
		WallOfShame wallOfShame = WallOfShame.openExistingDirectory(
				WallFiles.defaultsInDirectory(Util.PAGES_DIRECTORY),
				GitInformation.simple(
						Util.GIT_REPOSITORY_URL,
						Util.PAGES_DIRECTORY,
						Util.GIT_USER_NAME,
						Util.GIT_PASSWORD,
						Util.GIT_EMAIL));
		ComputationThread outputResultsComputation = new ComputationThread(outputResults(taskManager, wallOfShame));

		LOGGER.info("Transforming results to output...");
		Thread.currentThread().setName("Manage Queues");
		outputResultsComputation.start();

		// Manage queues, so that the loaded data gets processed.
		// But if the data contains unresolved or unanalyzed artifacts/projects,
		// the program will never stop because there are no computations to complete those tasks.
		// So we just stop after a couple of seconds.
		try {
			Executors.newSingleThreadExecutor()
					.submit(taskManager::manageQueues)
					.get(5, SECONDS);
		} catch (TimeoutException ex) {
			taskManager.stopManagingQueue();
		}

		outputResultsComputation.notifyAbort();

		LOGGER.info("Writing results...");
		wallOfShame.write();

		LOGGER.info("All done.");
	}

	private static TaskSink<CompletedArtifact> outputResults(AnalysisTaskManager taskManager, WallOfShame wallOfShame) {
		return new TaskSink<>(
				"Output Results",
				taskManager::getNextToOutput,
				artifact -> {
					wallOfShame.addArtifacts(artifact);
					return null;
				},
				(artifact, error) -> LOGGER.error("Failed to write result '" + artifact.coordinates() + "'.", error));
	}

}
