package org.codefx.jwos.analysis;

import org.codefx.jwos.analysis.channel.TaskChannel;
import org.codefx.jwos.analysis.task.Task;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.CompletedArtifact;
import org.codefx.jwos.artifact.CompletedArtifact.CompletedArtifactBuilder;
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

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.codefx.jwos.Util.toImmutableSet;
import static org.codefx.jwos.analysis.task.TaskStateIdentifier.FAILED;
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
	private final AnalysisTaskChannels channels;
	private final Bookkeeping bookkeeping;

	private AnalysisTaskManager(AnalysisTaskChannels channels) {
		this.state = new AnalysisGraph();
		this.channels = requireNonNull(channels, "The argument 'channels' must not be null.");
		this.bookkeeping = new Bookkeeping();
	}

	public AnalysisTaskManager(AnalysisPersistence persistence) {
		this(new PersistenceAnalysisTaskChannels(persistence));
	}

	public AnalysisTaskManager() {
		this(new SimpleAnalysisTaskChannels());
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
	 * Queues new tasks and processes results and errors by updating {@link #state}.
	 */
	private void updateState() {
		queueTasks();
		processAnswers();
		completeAnalysisAndQueueResults();
	}

	// - SEND OUT

	private void queueTasks() {
		state.projectNodes().forEach(this::queueTasksForProjectNode);
		state.artifactNodes()
				.filter(this::notYetCompleted)
				.forEach(this::queueTasksForArtifactNode);
	}

	private boolean notYetCompleted(ArtifactNode node) {
		return !node.completion().isFinished();
	}

	private void queueTasksForProjectNode(ProjectNode node) {
		queueTaskForProjectNode(node, ProjectNode::resolution, channels.resolveVersions());
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
		queueTaskForArtifactNode(node, ArtifactNode::download, channels.downloadArtifacts());
		queueAnalysisTaskForArtifactNode(node, channels.analyzeArtifacts());
		queueTaskForArtifactNode(node, ArtifactNode::resolution, channels.resolveDependencies());
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
		processAnswersFromChannel(channels.downloadArtifacts(), state::downloadOf);
		processAnswersFromChannel(channels.analyzeArtifacts(), state::analysisOf);
		processAnswersFromChannel(channels.resolveDependencies(), state::dependencyResolutionOf);
	}

	private void processAnswersFromNewProjects() {
		channels.addProjects().drainResults().forEach(this::processSuccessOfProjectDiscovery);
		channels.addProjects().drainErrors().forEach(this::processFailureOfProjectDiscovery);
	}

	private void processSuccessOfProjectDiscovery(ProjectCoordinates project) {
		TASKS_LOGGER.debug("Discovered project {}.", project.coordinates());
		state.addProject(project);
	}

	private void processFailureOfProjectDiscovery(Exception error) {
		TASKS_LOGGER.warn("Error while finding projects.", error);
	}

	private void processAnswersFromResolvedProjects() {
		channels.resolveVersions().drainResults().forEach(this::processSuccessOfVersionResolution);
		channels.resolveVersions().drainErrors().forEach(this::processFailureOfVersionResolution);
	}

	private void processSuccessOfVersionResolution(ResolvedProject project) {
		TASKS_LOGGER.debug("Storing {} result for {}.", channels.resolveVersions().taskName(), project.coordinates());
		state.versionResolutionOf(project).succeeded(project.result());
	}

	private void processFailureOfVersionResolution(FailedProject project) {
		TASKS_LOGGER.debug("Storing {} failure for {}.", channels.resolveVersions().taskName(), project.coordinates());
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

	private void completeAnalysisAndQueueResults() {
		completeAnalysisRecursively(state.artifactNodes())
				.forEach(channels.outputResults()::sendTask);
	}

	private static Stream<CompletedArtifact> completeAnalysisRecursively(Stream<ArtifactNode> nodes) {
		return nodes
				.filter(AnalysisTaskManager::readyForCompletion)
				.flatMap(AnalysisTaskManager::completeNodeAndRecurseToDependents);
	}

	private static boolean readyForCompletion(ArtifactNode node) {
		// the analysis is done if it is done (surprise) _or_ if the file could not be downloaded,
		// because in that case the analysis can not happen at all
		boolean analysisFinished = node.analysis().isFinished() || node.download().identifier() == FAILED;
		return analysisFinished
				&& node.resolution().isFinished()
				&& !node.completion().isFinished()
				&& allDependeesCompleted(node);
	}

	private static boolean allDependeesCompleted(ArtifactNode node) {
		return node.resolution().identifier() == FAILED
				|| node
					.resolution()
					.result().stream()
					.allMatch(dependee -> dependee.completion().isFinished());
	}

	private static Stream<CompletedArtifact> completeNodeAndRecurseToDependents(ArtifactNode node) {
		CompletedArtifact completedArtifact = completeNode(node);
		node.completion().succeeded(completedArtifact);

		return concat(
				of(completedArtifact),
				completeAnalysisRecursively(node.dependents())
		);
	}

	private static CompletedArtifact completeNode(ArtifactNode node) {
		CompletedArtifactBuilder builder = CompletedArtifact.forArtifact(node.coordinates());
		addViolations(node, builder);
		addDependees(node, builder);
		return builder.build();
	}

	private static void addViolations(ArtifactNode node, CompletedArtifactBuilder builder) {
		if (node.analysis().identifier() == FAILED)
			builder.violationAnalysisFailedWith(node.analysis().error());
		else if (node.download().identifier() == FAILED)
			builder.violationAnalysisFailedWith(node.download().error());
		else if (node.analysis().identifier() == SUCCEEDED)
			builder.withViolations(node.analysis().result());
	}

	private static void addDependees(ArtifactNode node, CompletedArtifactBuilder builder) {
		if (node.resolution().identifier() == FAILED)
			builder.dependeeResolutionFailedWith(node.resolution().error());
		else if (node.resolution().identifier() == SUCCEEDED) {
			builder.withDependees(node
					.resolution()
					.result().stream()
					.map(analyzedArtifact -> analyzedArtifact.completion().result())
					.collect(toImmutableSet()));
		}
	}

	// UPDATE PERSISTENCE

	private void updatePersistence() {
		if (channels instanceof PersistenceAnalysisTaskChannels)
			((PersistenceAnalysisTaskChannels) channels).updatePersistence();
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
		channels.addProjects().sendResult(project);
	}

	public void findingProjectFailed(Exception error) throws InterruptedException {
		channels.addProjects().sendError(error);
	}

	public ProjectCoordinates getNextToResolveVersions() throws InterruptedException {
		return getTaskAndStart(channels.resolveVersions(), state::versionResolutionOf, ProjectCoordinates::coordinates);
	}

	public void resolvedVersions(ResolvedProject project) throws InterruptedException {
		TASKS_LOGGER.debug("Version resolution for {} succeeded: {}", project.coordinates(), project.versions());
		channels.resolveVersions().sendResult(project);
	}

	public void versionResolutionFailed(FailedProject project) throws InterruptedException {
		TASKS_LOGGER.warn("Version resolution for {} failed: {}", project.coordinates(), project.error().toString());
		channels.resolveVersions().sendError(project);
	}

	public ArtifactCoordinates getNextToDownload() throws InterruptedException {
		return getArtifactTaskAndStart(channels.downloadArtifacts(), state::downloadOf);
	}

	public void downloaded(DownloadedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.debug("Download for {} succeeded: {}", artifact.coordinates(), artifact.path());
		channels.downloadArtifacts().sendResult(artifact);
	}

	public void downloadFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.warn("Download for {} failed: {}", artifact.coordinates(), artifact.error().toString());
		channels.downloadArtifacts().sendError(artifact);
	}

	public DownloadedArtifact getNextToAnalyze() throws InterruptedException {
		return getArtifactTaskAndStart(channels.analyzeArtifacts(), state::analysisOf);
	}

	public void analyzed(AnalyzedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.debug("Analysis for {} succeeded: {}", artifact.coordinates(), artifact.violations());
		channels.analyzeArtifacts().sendResult(artifact);
	}

	public void analysisFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.warn("Analysis for {} failed: {}", artifact.coordinates(), artifact.error().toString());
		channels.analyzeArtifacts().sendError(artifact);
	}

	public ArtifactCoordinates getNextToResolveDependencies() throws InterruptedException {
		return getArtifactTaskAndStart(channels.resolveDependencies(), state::dependencyResolutionOf);
	}

	public void resolvedDependencies(ResolvedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER.debug("Dependency resolution for {} succeeded: {}", artifact.coordinates(), artifact.dependees());
		channels.resolveDependencies().sendResult(artifact);
	}

	public void dependencyResolutionFailed(FailedArtifact artifact) throws InterruptedException {
		TASKS_LOGGER
				.warn("Dependency resolution for {} failed: {}", artifact.coordinates(), artifact.error().toString());
		channels.resolveDependencies().sendError(artifact);
	}

	public CompletedArtifact getNextToOutput() throws InterruptedException {
		return getArtifactTaskAndStart(channels.outputResults(), state::outputOf);
	}

	/**
	 * Calls {@link #updateState()} and logs graph and queue sizes.
	 */
	private class Bookkeeping {

		private static final int QUEUE_SIZE_LOG_INTERVAL = 20;
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
				updatePersistence();
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
			runsToNextLog = 0;
		}

		private void maybeLogGraphAndQueueSizes() {
			runsToNextLog--;
			if (runsToNextLog > 0)
				return;

			String message = "\nNodes:\n"
					+ logGraphSize(state.projectNodes(), "projects")
					+ logGraphSize(state.artifactNodes(), "artifacts")
					+ "Waiting tasks:\n"
					+ logQueueSize(channels.addProjects())
					+ logQueueSize(channels.resolveVersions())
					+ logQueueSize(channels.downloadArtifacts())
					+ logQueueSize(channels.analyzeArtifacts())
					+ logQueueSize(channels.resolveDependencies())
					+ logQueueSize(channels.outputResults());
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
