package org.codefx.jwos.analysis;

import org.codefx.jwos.analysis.task.Task;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifactTask;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.codefx.jwos.analysis.task.TaskStateIdentifier.NOT_COMPUTED;

public class AnalysisTaskManager {

	private static final Logger TASKS_LOGGER = LoggerFactory.getLogger("Analysis Tasks");
	private static final Logger THREAD_LOGGER = LoggerFactory.getLogger("Analysis Thread");
	private static final String CHANNEL_STATUS_MESSAGE_FORMAT = " - %d are waiting for %s";

	private final AnalysisGraph state;

	private final TaskChannel<ProjectCoordinates, ResolvedProject, FailedProject> resolveVersions;
	private final TaskChannel<ArtifactCoordinates, DownloadedArtifact, FailedArtifact> download;
	private final TaskChannel<ArtifactCoordinates, AnalyzedArtifact, FailedArtifact> analyze;
	private final TaskChannel<ArtifactCoordinates, ResolvedArtifact, FailedArtifact> resolveDependencies;

	private final Bookkeeping bookkeeping;

	public AnalysisTaskManager(
			Collection<ProjectCoordinates> resolvedProjects, Collection<DeeplyAnalyzedArtifact> analyzedArtifacts) {
		state = new AnalysisGraph(resolvedProjects, analyzedArtifacts);

		resolveVersions = new TaskChannel<>("version resolution");
		download = new TaskChannel<>("download");
		analyze = new TaskChannel<>("analysis");
		resolveDependencies = new TaskChannel<>("dependency resolution");
		bookkeeping = new Bookkeeping();
	}

	public AnalysisTaskManager() {
		this(emptySet(), emptySet());
	}

	/**
	 * A blocking call which queues tasks and processes answers.
	 */
	public void manageQueues() {
		bookkeeping.run();
	}

	public void stopManagingQueue() {
		bookkeeping.abort();
	}

	private void queueTasks() {
		state.projectNodes().forEach(this::queueTasksForProjectNode);
		state.artifactNodes().forEach(this::queueTasksForArtifactNode);
	}

	private void queueTasksForProjectNode(ProjectNode node) {
		queueTaskForProjectNode(node, ProjectNode::resolution, resolveVersions);
	}

	private static void queueTaskForProjectNode(
			ProjectNode node,
			Function<ProjectNode, Task<?>> getTask,
			TaskChannel<ProjectCoordinates, ?, ?> channel) {
		Task<?> task = getTask.apply(node);
		if (task.identifier() == NOT_COMPUTED) {
			TASKS_LOGGER.info(format("Queuing %s for %s.", node.coordinates(), channel.taskName()));
			task.queued();
			channel.sendTask(node.coordinates());
		}
	}

	private void queueTasksForArtifactNode(ArtifactNode node) {
		queueTaskForArtifactNode(node, ArtifactNode::download, download);
		queueTaskForArtifactNode(node, ArtifactNode::analysis, analyze);
		queueTaskForArtifactNode(node, ArtifactNode::resolution, resolveDependencies);
	}

	private static void queueTaskForArtifactNode(
			ArtifactNode node,
			Function<ArtifactNode, Task<?>> getTask,
			TaskChannel<ArtifactCoordinates, ?, ?> channel) {
		Task<?> task = getTask.apply(node);
		if (task.identifier() == NOT_COMPUTED) {
			TASKS_LOGGER.info(format("Queuing %s for %s.", node.coordinates(), channel.taskName()));
			task.queued();
			channel.sendTask(node.coordinates());
		}
	}

	private void processAnswers() {
		processAnswersFromChannel(download, state::downloadOf);
		processAnswersFromChannel(analyze, state::analysisOf);
		processAnswersFromChannel(resolveDependencies, state::dependencyResolutionOf);
	}

	private static <R> void processAnswersFromChannel(
			TaskChannel<?, ? extends IdentifiesArtifactTask<R>, FailedArtifact> channel,
			Function<IdentifiesArtifact, Task<R>> getTask) {
		channel.drainResults().forEach(artifact -> processSuccessOfTask(artifact, getTask, channel.taskName()));
		channel.drainErrors().forEach(artifact -> processFailureOfTask(artifact, getTask, channel.taskName()));
	}

	private static <R> void processSuccessOfTask(
			IdentifiesArtifactTask<R> artifact,
			Function<IdentifiesArtifact, Task<R>> getTask,
			String taskName) {
		TASKS_LOGGER.info(format("Processing %s result for %s.", taskName, artifact.coordinates()));
		getTask.apply(artifact).succeeded(artifact.result());
	}

	private static <R> void processFailureOfTask(
			FailedArtifact artifact,
			Function<IdentifiesArtifact, Task<R>> getTask,
			String taskName) {
		TASKS_LOGGER.info(format("Processing %s failure for %s.", taskName, artifact.coordinates()));
		getTask.apply(artifact).failed(artifact.result());
	}

	// QUERY & UPDATE

	private static <T> T getTaskAndStart(
			TaskChannel<T, ?, ?> channel,
			Function<T, Task<?>> getTask,
			Function<T, Object> getCoordinates)
			throws InterruptedException {
		T task = channel.getTask();
		TASKS_LOGGER.info(format("Starting %s for %s.", channel.taskName(), getCoordinates.apply(task)));
		getTask.apply(task).started();
		return task;
	}

	public ProjectCoordinates getNextToResolveVersions() throws InterruptedException {
		return getTaskAndStart(resolveVersions, state::versionResolutionOf, ProjectCoordinates::coordinates);
	}

	public void resolvedVersions(ResolvedProject project) throws InterruptedException {
		TASKS_LOGGER.info(
				format("Version resolution for %s succeeded: %s", project.coordinates(), project.versions()));
		resolveVersions.addResult(project);
	}

	public void versionResolutionFailed(FailedProject project) throws InterruptedException {
		TASKS_LOGGER.info(format("Version resolution for %s failed: %s", project.coordinates(), project.error()));
		resolveVersions.addError(project);
	}

	public ArtifactCoordinates getNextToDownload() throws InterruptedException {
		return getTaskAndStart(download, state::downloadOf, ArtifactCoordinates::coordinates);
	}

	public void downloaded(DownloadedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(format("Download for %s succeeded: %s", artifact.coordinates(), artifact.path()));
		download.addResult(artifact);
	}

	public void downloadFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(format("Download for %s failed: %s", artifact.coordinates(), artifact.error()));
		download.addError(artifact);
	}

	public ArtifactCoordinates getNextToAnalyze() throws InterruptedException {
		return getTaskAndStart(analyze, state::analysisOf, ArtifactCoordinates::coordinates);
	}

	public void analyzed(AnalyzedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(format("Analysis for %s succeeded: %s", artifact.coordinates(), artifact.violations()));
		analyze.addResult(artifact);
	}

	public void analysisFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(format("Analysis for %s failed: %s", artifact.coordinates(), artifact.error()));
		analyze.addError(artifact);
	}

	public ArtifactCoordinates getNextToResolveDependencies() throws InterruptedException {
		return getTaskAndStart(resolveDependencies, state::dependencyResolutionOf, ArtifactCoordinates::coordinates);
	}

	public void resolvedDependencies(ResolvedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(
				format("Dependency resolution for %s succeeded: %s", artifact.coordinates(), artifact.dependees()));
		resolveDependencies.addResult(artifact);
	}

	public void dependencyResolutionFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(format("Dependency resolution for %s failed: %s", artifact.coordinates(), artifact.error()));
		resolveDependencies.addError(artifact);
	}

	private class Bookkeeping {

		private boolean running;
		private boolean aborted;

		public void run() {
			startRunning();
			THREAD_LOGGER.info("Start managing queues.");

			while (!aborted) {
				queueTasks();
				processAnswers();
				logQueueSizes();
				sleepAndAbortWhenInterrupted();
			}

			stopRunning();
			THREAD_LOGGER.info("Queue management stopped.");
		}

		private void startRunning() {
			if (running)
				throw new IllegalStateException("The bookkeeping thread is already running.");
			running = true;
		}

		private void logQueueSizes() {
			String message = "Waiting tasks:\n"
					+ logQueueSize(resolveVersions)
					+ logQueueSize(download)
					+ logQueueSize(analyze)
					+ logQueueSize(resolveDependencies);
			THREAD_LOGGER.info(message);
		}

		private String logQueueSize(TaskChannel<?, ?, ?> channel) {
			return format(CHANNEL_STATUS_MESSAGE_FORMAT, channel.nrOfWaitingTasks(), channel.taskName()) + "\n";
		}

		private void sleepAndAbortWhenInterrupted() {
			THREAD_LOGGER.info("Done. Sleeping for a while...");
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				THREAD_LOGGER.warn("Who woke me? Stopping queue management...");
				Thread.currentThread().interrupt();
				aborted = true;
			}
		}

		private void stopRunning() {
			running = false;
			aborted = false;
		}

		public void abort() {
			THREAD_LOGGER.info("Stopping queue management...");
			aborted = true;
		}
	}

}
