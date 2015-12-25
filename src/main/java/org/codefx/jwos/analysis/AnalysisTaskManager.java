package org.codefx.jwos.analysis;

import org.codefx.jwos.analysis.state.Computation;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.FailedProject;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifactComputation;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.artifact.ResolvedProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.codefx.jwos.analysis.state.ComputationStateIdentifier.NOT_COMPUTED;

public class AnalysisTaskManager {

	private static final Logger LOGGER = LoggerFactory.getLogger("Analysis Task Manager");

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
			Function<ProjectNode, Computation<?>> getComputation,
			TaskChannel<ProjectCoordinates, ?, ?> channel) {
		Computation<?> computation = getComputation.apply(node);
		if (computation.state() == NOT_COMPUTED) {
			LOGGER.info(format("Queuing %s for %s.", node.coordinates(), channel.taskName()));
			computation.queued();
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
			Function<ArtifactNode, Computation<?>> getComputation,
			TaskChannel<ArtifactCoordinates, ?, ?> channel) {
		Computation<?> computation = getComputation.apply(node);
		if (computation.state() == NOT_COMPUTED) {
			LOGGER.info(format("Queuing %s for %s.", node.coordinates(), channel.taskName()));
			computation.queued();
			channel.sendTask(node.coordinates());
		}
	}

	private void processAnswers() {
		processAnswersFromChannel(download, state::downloadOf);
		processAnswersFromChannel(analyze, state::analysisOf);
		processAnswersFromChannel(resolveDependencies, state::dependencyResolutionOf);
	}

	private static <R> void processAnswersFromChannel(
			TaskChannel<?, ? extends IdentifiesArtifactComputation<R>, FailedArtifact> channel,
			Function<IdentifiesArtifact, Computation<R>> getComputation) {
		channel.drainResults().forEach(artifact -> processSuccessOfTask(artifact, getComputation, channel.taskName()));
		channel.drainErrors().forEach(artifact -> processFailureOfTask(artifact, getComputation, channel.taskName()));
	}

	private static <R> void processSuccessOfTask(
			IdentifiesArtifactComputation<R> artifact,
			Function<IdentifiesArtifact, Computation<R>> getComputation,
			String taskName) {
		LOGGER.info(format("Processing %s result for %s.", taskName, artifact.coordinates()));
		getComputation.apply(artifact).succeeded(artifact.result());
	}

	private static <R> void processFailureOfTask(
			FailedArtifact artifact,
			Function<IdentifiesArtifact, Computation<R>> getComputation,
			String taskName) {
		LOGGER.info(format("Processing %s failure for %s.", taskName, artifact.coordinates()));
		getComputation.apply(artifact).failed(artifact.result());
	}

	// QUERY & UPDATE

	private static <C> C getTaskAndStart(
			TaskChannel<C, ?, ?> channel,
			Function<C, Computation<?>> getComputation,
			Function<C, Object> getCoordinates)
			throws InterruptedException {
		C task = channel.getTask();
		LOGGER.info(format("Starting %s for %s.", channel.taskName(), getCoordinates.apply(task)));
		getComputation.apply(task).started();
		return task;
	}

	public ProjectCoordinates getNextToResolveVersions() throws InterruptedException {
		return getTaskAndStart(resolveVersions, state::versionResolutionOf, ProjectCoordinates::coordinates);
	}

	public void resolvedDependencies(ResolvedProject project) throws InterruptedException {
		LOGGER.info(format("Version resolution for %s succeeded: %s", project.coordinates(), project.versions()));
		resolveVersions.addResult(project);
	}

	public void dependencyResolutionFailed(FailedProject project) throws InterruptedException {
		LOGGER.info(format("Version resolution for %s failed: %s", project.coordinates(), project.error()));
		resolveVersions.addError(project);
	}

	public ArtifactCoordinates getNextToDownload() throws InterruptedException {
		return getTaskAndStart(download, state::downloadOf, ArtifactCoordinates::coordinates);
	}

	public void downloaded(DownloadedArtifact artifact) throws InterruptedException {
		LOGGER.info(format("Download for %s succeeded: %s", artifact.coordinates(), artifact.path()));
		download.addResult(artifact);
	}

	public void downloadFailed(FailedArtifact artifact) throws InterruptedException {
		LOGGER.info(format("Download for %s failed: %s", artifact.coordinates(), artifact.error()));
		download.addError(artifact);
	}

	public ArtifactCoordinates getNextToAnalyze() throws InterruptedException {
		return getTaskAndStart(analyze, state::analysisOf, ArtifactCoordinates::coordinates);
	}

	public void analyzed(AnalyzedArtifact artifact) throws InterruptedException {
		LOGGER.info(format("Analysis for %s succeeded: %s", artifact.coordinates(), artifact.violations()));
		analyze.addResult(artifact);
	}

	public void analysisFailed(FailedArtifact artifact) throws InterruptedException {
		LOGGER.info(format("Analysis for %s failed: %s", artifact.coordinates(), artifact.error()));
		analyze.addError(artifact);
	}

	public ArtifactCoordinates getNextToResolveDependencies() throws InterruptedException {
		return getTaskAndStart(resolveDependencies, state::dependencyResolutionOf, ArtifactCoordinates::coordinates);
	}

	public void resolvedDependencies(ResolvedArtifact artifact) throws InterruptedException {
		LOGGER.info(format("Dependency resolution for %s succeeded: %s", artifact.coordinates(), artifact.dependees
				()));
		resolveDependencies.addResult(artifact);
	}

	public void dependencyResolutionFailed(FailedArtifact artifact) throws InterruptedException {
		LOGGER.info(format("Dependency resolution for %s failed: %s", artifact.coordinates(), artifact.error()));
		resolveDependencies.addError(artifact);
	}

	private class Bookkeeping {

		private boolean running;
		private boolean aborted;

		public void run() {
			startRunning();
			LOGGER.info("Start managing queues.");

			while (!aborted) {
				queueTasks();
				processAnswers();
				sleepAndAbortWhenInterrupted();
			}

			stopRunning();
			LOGGER.info("Queue management stopped.");
		}

		private void startRunning() {
			if (running)
				throw new IllegalStateException("The bookkeeping thread is already running.");
			running = true;
		}

		private void sleepAndAbortWhenInterrupted() {
			LOGGER.info("Done. Sleeping for a while...");
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				LOGGER.warn("Who woke me? Stopping queue management...");
				Thread.currentThread().interrupt();
				aborted = true;
			}
		}

		private void stopRunning() {
			running = false;
			aborted = false;
		}

		public void abort() {
			LOGGER.info("Stopping queue management...");
			aborted = true;
		}
	}

}
