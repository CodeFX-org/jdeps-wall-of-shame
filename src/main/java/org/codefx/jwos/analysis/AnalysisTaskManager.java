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
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.codefx.jwos.Util.toImmutableSet;
import static org.codefx.jwos.analysis.task.TaskStateIdentifier.NOT_COMPUTED;
import static org.codefx.jwos.analysis.task.TaskStateIdentifier.SUCCEEDED;

/**
 * Manages the tasks that have to be performed in order to analyse projects and artifacts.
 * <p>
 * Tasks can consist of resolving a project's version, downloading an artifact, or running JDeps on it.
 * Tasks can be received from methods like {@link #getNextToAnalyze()} and upon completion (success or failure)
 * returned with, e.g., {@link #analyzed(AnalyzedArtifact)} or {@link #analysisFailed(FailedArtifact)}. These methods
 * might block if there is currently no task or the queue of completed tasks is full.
 * <p>
 * This task manager is thread safe.
 */
public class AnalysisTaskManager {

	private static final Logger TASKS_LOGGER = LoggerFactory.getLogger("Analysis Tasks");
	private static final Logger THREAD_LOGGER = LoggerFactory.getLogger("Analysis Thread");
	private static final String GRAPH_STATUS_MESSAGE_FORMAT = " - %5d %s";
	private static final String CHANNEL_STATUS_MESSAGE_FORMAT = " - %5d are waiting for %s";

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
	 *
	 * @see Bookkeeping
	 */
	public void manageQueues() {
		bookkeeping.run();
	}

	public void stopManagingQueue() {
		bookkeeping.abort();
	}

	// UPDATE STATE

	/**
	 * Queues new tasks and processes results and failures by updating {@link #state}.
	 */
	private void updateState() {
		queueTasks();
		processAnswers();
		finishDeepAnalysisAndQueueResults();
	}

	// - SEND OUT

	private void queueTasks() {
		state.projectNodes().forEach(this::queueTasksForProjectNode);
		state.artifactNodes()
				.filter(this::notYetDeeplyAnalyzed)
				.forEach(this::queueTasksForArtifactNode);
	}

	private boolean notYetDeeplyAnalyzed(ArtifactNode node) {
		return node.deepAnalysis().identifier() != SUCCEEDED;
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
			TASKS_LOGGER.debug("Queuing {} for {}.", node.coordinates(), channel.taskName());
			task.queued();
			channel.sendTask(node.coordinates());
		}
	}

	private void queueTasksForArtifactNode(ArtifactNode node) {
		queueTaskForArtifactNode(node, ArtifactNode::download, download);
		queueAnalysisTaskForArtifactNode(node, analyze);
		queueTaskForArtifactNode(node, ArtifactNode::resolution, resolveDependencies);
	}

	private static void queueTaskForArtifactNode(
			ArtifactNode node,
			Function<ArtifactNode, Task<?>> getTask,
			TaskChannel<ArtifactCoordinates, ?, ?> channel) {
		Task<?> task = getTask.apply(node);
		if (task.identifier() == NOT_COMPUTED) {
			TASKS_LOGGER.debug("Queuing {} for {}.", node.coordinates(), channel.taskName());
			task.queued();
			channel.sendTask(node.coordinates());
		}
	}

	private static void queueAnalysisTaskForArtifactNode(
			ArtifactNode node, TaskChannel<DownloadedArtifact, AnalyzedArtifact, FailedArtifact> channel) {
		Task<Path> downloadTask = node.download();
		Task<?> analysisTask = node.analysis();
		if (downloadTask.identifier() == SUCCEEDED && analysisTask.identifier() == NOT_COMPUTED) {
			TASKS_LOGGER.debug("Queuing {} for {}.", node.coordinates(), channel.taskName());
			analysisTask.queued();
			channel.sendTask(new DownloadedArtifact(node.coordinates(), downloadTask.result()));
		}
	}

	// - RECEIVE

	private void processAnswers() {
		processAnswersFromNewProjects();
		processAnswersFromResolvedProjects();
		processAnswersFromChannel(download, state::downloadOf);
		processAnswersFromChannel(analyze, state::analysisOf);
		processAnswersFromChannel(resolveDependencies, state::dependencyResolutionOf);
	}

	private void processAnswersFromNewProjects() {
		addProject.drainResults().forEach(this::processSuccessOfProjectDiscovery);
		addProject.drainErrors().forEach(this::processFailureOfProjectDiscovery);
	}

	private void processSuccessOfProjectDiscovery(ProjectCoordinates project) {
		TASKS_LOGGER.debug("Discovered project {}.", project.coordinates());
		state.addProject(project);
	}

	private void processFailureOfProjectDiscovery(Exception error) {
		TASKS_LOGGER.warn("Error while finding projects.", error);
	}

	private void processAnswersFromResolvedProjects() {
		resolveVersions.drainResults().forEach(this::processSuccessOfVersionResolution);
		resolveVersions.drainErrors().forEach(this::processFailureOfVersionResolution);
	}

	private void processSuccessOfVersionResolution(ResolvedProject project) {
		TASKS_LOGGER.debug("Storing {} result for {}.", resolveVersions.taskName(), project.coordinates());
		state.versionResolutionOf(project).succeeded(project.result());
	}

	private void processFailureOfVersionResolution(FailedProject project) {
		TASKS_LOGGER.debug("Storing {} failure for {}.", resolveVersions.taskName(), project.coordinates());
		state.versionResolutionOf(project).failed(project.error());
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
		TASKS_LOGGER.debug("Storing {} result for {}.", taskName, artifact.coordinates());
		getTask.apply(artifact).succeeded(artifact.result());
	}

	private static <R> void processFailureOfTask(
			FailedArtifact artifact,
			Function<IdentifiesArtifact, Task<R>> getTask,
			String taskName) {
		TASKS_LOGGER.debug("Storing {} failure for {}.", taskName, artifact.coordinates());
		getTask.apply(artifact).failed(artifact.result());
	}

	// - OUTPUT FINISHED

	private void finishDeepAnalysisAndQueueResults() {
		finishDeepAnalysisRecursively(state.artifactNodes()).collect(toList()).forEach(outputResults::sendTask);
	}

	private static Stream<DeeplyAnalyzedArtifact> finishDeepAnalysisRecursively(Stream<ArtifactNode> nodes) {
		return nodes
				.filter(AnalysisTaskManager::readyForDeepAnalysis)
				.flatMap(AnalysisTaskManager::deeplyAnalyzeNodeAndRecurseToDependents);
	}

	private static boolean readyForDeepAnalysis(ArtifactNode node) {
		return node.analysis().identifier() == SUCCEEDED
				&& node.resolution().identifier() == SUCCEEDED
				&& node.deepAnalysis().identifier() != SUCCEEDED
				&& allDependeesDeeplyAnalyzed(node);
	}

	private static boolean allDependeesDeeplyAnalyzed(ArtifactNode node) {
		Predicate<ArtifactNode> deeplyAnalyzed = dependee -> dependee.deepAnalysis().identifier() == SUCCEEDED;
		return node.resolution().result().stream().allMatch(deeplyAnalyzed);
	}

	private static Stream<DeeplyAnalyzedArtifact> deeplyAnalyzeNodeAndRecurseToDependents(ArtifactNode node) {
		DeeplyAnalyzedArtifact deeplyAnalyzedArtifact = deeplyAnalyzeNode(node);
		node.deepAnalysis().succeeded(deeplyAnalyzedArtifact);

		return concat(
				of(deeplyAnalyzedArtifact),
				finishDeepAnalysisRecursively(node.dependents())
		);
	}

	private static DeeplyAnalyzedArtifact deeplyAnalyzeNode(ArtifactNode node) {
		ImmutableSet<Violation> violations = node.analysis().result();
		ImmutableSet<DeeplyAnalyzedArtifact> dependees = node
				.resolution()
				.result().stream()
				.map(analyzedArtifact -> analyzedArtifact.deepAnalysis().result())
				.collect(toImmutableSet());
		MarkInternalDependencies marker =
				getMarkerForInternalDependencies(violations, dependees);
		return new DeeplyAnalyzedArtifact(node.coordinates(), marker, violations, dependees);
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

	// QUERY & UPDATE

	// all of this could be replaced by creating an outward facing front for channels and exposing them;
	// I like this better

	private static <T> T getTaskAndStart(
			TaskChannel<T, ?, ?> channel,
			Function<T, Task<?>> getTask,
			Function<T, Object> getCoordinates)
			throws InterruptedException {
		T task = channel.getTask();
		TASKS_LOGGER.debug("Starting {} for {}.", channel.taskName(), getCoordinates.apply(task));
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
		TASKS_LOGGER.debug("Version resolution for {} succeeded: {}", project.coordinates(), project.versions());
		resolveVersions.sendResult(project);
	}

	public void versionResolutionFailed(FailedProject project) throws InterruptedException {
		TASKS_LOGGER.warn("Version resolution for {} failed: {}", project.coordinates(), project.error().toString());
		resolveVersions.sendError(project);
	}

	public ArtifactCoordinates getNextToDownload() throws InterruptedException {
		return getArtifactTaskAndStart(download, state::downloadOf);
	}

	public void downloaded(DownloadedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.debug("Download for {} succeeded: {}", artifact.coordinates(), artifact.path());
		download.sendResult(artifact);
	}

	public void downloadFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.warn("Download for {} failed: {}", artifact.coordinates(), artifact.error().toString());
		download.sendError(artifact);
	}

	public DownloadedArtifact getNextToAnalyze() throws InterruptedException {
		return getArtifactTaskAndStart(analyze, state::analysisOf);
	}

	public void analyzed(AnalyzedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.debug("Analysis for {} succeeded: {}", artifact.coordinates(), artifact.violations());
		analyze.sendResult(artifact);
	}

	public void analysisFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.warn("Analysis for {} failed: {}", artifact.coordinates(), artifact.error().toString());
		analyze.sendError(artifact);
	}

	public ArtifactCoordinates getNextToResolveDependencies() throws InterruptedException {
		return getArtifactTaskAndStart(resolveDependencies, state::dependencyResolutionOf);
	}

	public void resolvedDependencies(ResolvedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.debug("Dependency resolution for {} succeeded: {}", artifact.coordinates(), artifact.dependees());
		resolveDependencies.sendResult(artifact);
	}

	public void dependencyResolutionFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER
				.warn("Dependency resolution for {} failed: {}", artifact.coordinates(), artifact.error().toString());
		resolveDependencies.sendError(artifact);
	}

	public DeeplyAnalyzedArtifact getNextToOutput() throws InterruptedException {
		return getArtifactTaskAndStart(outputResults, state::outputOf);
	}

	/**
	 * Calls {@link #updateState()} and logs graph and queue sizes.
	 */
	private class Bookkeeping {

		private static final int QUEUE_SIZE_LOG_INTERVAL = 200;
		private static final long SLEEP_TIME_IN_MS = 50;

		private int runsToNextLog;

		private boolean running;
		private boolean aborted;

		/**
		 * Executes bookkeeping; does not return so it should be called in a separate thread.
		 */
		public void run() {
			startRunning();
			THREAD_LOGGER.info("Start managing queues.");

			while (!aborted) {
				updateState();
				maybeLogGraphAndQueueSizes();
				sleepAndAbortWhenInterrupted();
			}

			stopRunning();
			THREAD_LOGGER.info("Queue management stopped.");
		}

		private void startRunning() {
			if (running)
				throw new IllegalStateException("The bookkeeping thread is already running.");
			running = true;
			runsToNextLog = QUEUE_SIZE_LOG_INTERVAL;
		}

		private void maybeLogGraphAndQueueSizes() {
			runsToNextLog--;
			if (runsToNextLog > 0)
				return;

			String message = "\nNodes:\n"
					+ logGraphSize(state.projectNodes(), "projects")
					+ logGraphSize(state.artifactNodes(), "artifacts")
					+ "Waiting tasks:\n"
					+ logQueueSize(addProject)
					+ logQueueSize(resolveVersions)
					+ logQueueSize(download)
					+ logQueueSize(analyze)
					+ logQueueSize(resolveDependencies)
					+ logQueueSize(outputResults);
			THREAD_LOGGER.info(message);
			runsToNextLog = QUEUE_SIZE_LOG_INTERVAL;
		}

		private String logGraphSize(Stream<?> nodes, String graphName) {
			return format(GRAPH_STATUS_MESSAGE_FORMAT, nodes.count(), graphName) + "\n";
		}

		private String logQueueSize(TaskChannel<?, ?, ?> channel) {
			return format(CHANNEL_STATUS_MESSAGE_FORMAT, channel.nrOfWaitingTasks(), channel.taskName()) + "\n";
		}

		private void sleepAndAbortWhenInterrupted() {
			THREAD_LOGGER.debug("Done. Sleeping for a while...");
			try {
				Thread.sleep(SLEEP_TIME_IN_MS);
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
