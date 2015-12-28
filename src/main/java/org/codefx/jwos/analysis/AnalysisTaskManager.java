package org.codefx.jwos.analysis;

import com.google.common.collect.ImmutableSet;
import org.codefx.jwos.analysis.task.Task;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifactTask;
import org.codefx.jwos.artifact.MarkInternalDependencies;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;
import org.codefx.jwos.jdeps.dependency.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.codefx.jwos.Util.toImmutableSet;
import static org.codefx.jwos.analysis.task.TaskStateIdentifier.NOT_COMPUTED;
import static org.codefx.jwos.analysis.task.TaskStateIdentifier.SUCCEEDED;

public class AnalysisTaskManager {

	private static final Logger TASKS_LOGGER = LoggerFactory.getLogger("Analysis Tasks");
	private static final Logger THREAD_LOGGER = LoggerFactory.getLogger("Analysis Thread");
	private static final String CHANNEL_STATUS_MESSAGE_FORMAT = " - %d are waiting for %s";

	private final AnalysisGraph state;

	private final TaskChannel<Void, ProjectCoordinates, Exception> addProject;
	private final TaskChannel<ProjectCoordinates, ResolvedProject, FailedProject> resolveVersions;
	private final TaskChannel<ArtifactCoordinates, DownloadedArtifact, FailedArtifact> download;
	private final TaskChannel<DownloadedArtifact, AnalyzedArtifact, FailedArtifact> analyze;
	private final TaskChannel<ArtifactCoordinates, ResolvedArtifact, FailedArtifact> resolveDependencies;
	private final TaskChannel<DeeplyAnalyzedArtifact, Void, Void> outputResults;

	private final Bookkeeping bookkeeping;

	public AnalysisTaskManager(
			Collection<ProjectCoordinates> resolvedProjects, Collection<DeeplyAnalyzedArtifact> analyzedArtifacts) {
		state = new AnalysisGraph(resolvedProjects, analyzedArtifacts);

		addProject = new TaskChannel<>("add project");
		resolveVersions = new TaskChannel<>("version resolution");
		download = new TaskChannel<>("download");
		analyze = new TaskChannel<>("analysis");
		resolveDependencies = new TaskChannel<>("dependency resolution");
		outputResults = new TaskChannel<>("output");

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

	private void updateState() {
		queueTasks();
		processAnswers();
		queueFinishedArtifacts();
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
		queueDownloadTaskForArtifactNode(node, analyze);
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

	private static void queueDownloadTaskForArtifactNode(
			ArtifactNode node, TaskChannel<DownloadedArtifact, AnalyzedArtifact, FailedArtifact> channel) {
		Task<Path> downloadTask = node.download();
		Task<?> analysisTask = node.analysis();
		if (downloadTask.identifier() == SUCCEEDED && analysisTask.identifier() == NOT_COMPUTED) {
			TASKS_LOGGER.info(format("Queuing %s for %s.", node.coordinates(), channel.taskName()));
			analysisTask.queued();
			channel.sendTask(new DownloadedArtifact(node.coordinates(), downloadTask.result()));
		}
	}

	private void processAnswers() {
		processAnswersFromNewProjects();
		processAnswersFromChannel(download, state::downloadOf);
		processAnswersFromChannel(analyze, state::analysisOf);
		processAnswersFromChannel(resolveDependencies, state::dependencyResolutionOf);
	}

	private void processAnswersFromNewProjects() {
		addProject.drainResults().forEach(state::addProject);
		addProject.drainErrors().forEach(error -> TASKS_LOGGER.warn("Error while finding projects.", error));
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

	private void queueFinishedArtifacts() {
		state.artifactNodes()
				.filter(AnalysisTaskManager::readyToFinish)
				.map(AnalysisTaskManager::finishArtifactRecursively)
				.forEach(outputResults::sendTask);
	}

	private static DeeplyAnalyzedArtifact finishArtifactRecursively(ArtifactNode node) {
		if (node.deepAnalysis().identifier() == SUCCEEDED)
			return node.deepAnalysis().result();

		ImmutableSet<Violation> violations = node.analysis().result();
		ImmutableSet<DeeplyAnalyzedArtifact> dependees = node
				.resolution()
				.result().stream()
				.map(AnalysisTaskManager::finishArtifactRecursively)
				.collect(toImmutableSet());
		MarkInternalDependencies marker =
				getMarkerForInternalDependencies(violations, dependees);
		DeeplyAnalyzedArtifact deeplyAnalyzedArtifact =
				new DeeplyAnalyzedArtifact(node.coordinates(), marker, violations, dependees);

		node.deepAnalysis().succeeded(deeplyAnalyzedArtifact);
		return deeplyAnalyzedArtifact;
	}

	private static MarkInternalDependencies getMarkerForInternalDependencies(
			ImmutableSet<Violation> violations, ImmutableSet<DeeplyAnalyzedArtifact> dependees) {
		if (violations.isEmpty())
			return dependees.stream()
					.map(DeeplyAnalyzedArtifact::marker)
					.reduce(MarkInternalDependencies.NONE, MarkInternalDependencies::combineWithDependee);
		else
			return MarkInternalDependencies.DIRECT;
	}

	private static boolean readyToFinish(ArtifactNode node) {
		return node.analysis().identifier() == SUCCEEDED
				&& node.resolution().identifier() == SUCCEEDED
				&& node.deepAnalysis().identifier() != SUCCEEDED;
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

	private static <T extends IdentifiesArtifact> T getArtifactTaskAndStart(
			TaskChannel<T, ?, ?> channel,
			Function<T, Task<?>> getTask)
			throws InterruptedException {
		return getTaskAndStart(channel, getTask, IdentifiesArtifact::coordinates);
	}

	public void addProject(ProjectCoordinates project) throws InterruptedException {
		addProject.sendResult(project);
	}

	public void findingProjectFailed(Exception error) throws InterruptedException {
		addProject.sendError(error);
	}

	public ProjectCoordinates getNextToResolveVersions() throws InterruptedException {
		return getTaskAndStart(resolveVersions, state::versionResolutionOf, ProjectCoordinates::coordinates);
	}

	public void resolvedVersions(ResolvedProject project) throws InterruptedException {
		TASKS_LOGGER.info(
				format("Version resolution for %s succeeded: %s", project.coordinates(), project.versions()));
		resolveVersions.sendResult(project);
	}

	public void versionResolutionFailed(FailedProject project) throws InterruptedException {
		TASKS_LOGGER.info(format("Version resolution for %s failed: %s", project.coordinates(), project.error()));
		resolveVersions.sendError(project);
	}

	public ArtifactCoordinates getNextToDownload() throws InterruptedException {
		return getArtifactTaskAndStart(download, state::downloadOf);
	}

	public void downloaded(DownloadedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(format("Download for %s succeeded: %s", artifact.coordinates(), artifact.path()));
		download.sendResult(artifact);
	}

	public void downloadFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(format("Download for %s failed: %s", artifact.coordinates(), artifact.error()));
		download.sendError(artifact);
	}

	public DownloadedArtifact getNextToAnalyze() throws InterruptedException {
		return getArtifactTaskAndStart(analyze, state::analysisOf);
	}

	public void analyzed(AnalyzedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(format("Analysis for %s succeeded: %s", artifact.coordinates(), artifact.violations()));
		analyze.sendResult(artifact);
	}

	public void analysisFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(format("Analysis for %s failed: %s", artifact.coordinates(), artifact.error()));
		analyze.sendError(artifact);
	}

	public ArtifactCoordinates getNextToResolveDependencies() throws InterruptedException {
		return getArtifactTaskAndStart(resolveDependencies, state::dependencyResolutionOf);
	}

	public void resolvedDependencies(ResolvedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(
				format("Dependency resolution for %s succeeded: %s", artifact.coordinates(), artifact.dependees()));
		resolveDependencies.sendResult(artifact);
	}

	public void dependencyResolutionFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.info(format("Dependency resolution for %s failed: %s", artifact.coordinates(), artifact.error()));
		resolveDependencies.sendError(artifact);
	}

	public DeeplyAnalyzedArtifact getNextToOutput() throws InterruptedException {
		return getArtifactTaskAndStart(outputResults, state::outputOf);
	}

	private class Bookkeeping {

		private boolean running;
		private boolean aborted;

		public void run() {
			startRunning();
			THREAD_LOGGER.info("Start managing queues.");

			while (!aborted) {
				updateState();
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
					+ logQueueSize(resolveDependencies)
					+ logQueueSize(outputResults);
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
