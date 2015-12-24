package org.codefx.jwos.analysis;

import org.codefx.jwos.analysis.state.Computation;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifactComputation;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static java.lang.String.format;
import static org.codefx.jwos.analysis.state.ComputationStateIdentifier.NOT_COMPUTED;

public class AnalysisTaskManager {

	private static final Logger LOGGER = LoggerFactory.getLogger("Analysis Task Manager");

	private final AnalysisGraph state;

	private final TaskChannel<ArtifactCoordinates, DownloadedArtifact, FailedArtifact> download;
	private final TaskChannel<ArtifactCoordinates, AnalyzedArtifact, FailedArtifact> analyze;
	private final TaskChannel<ArtifactCoordinates, ResolvedArtifact, FailedArtifact> resolve;

	private final Bookkeeping bookkeeping;

	public AnalysisTaskManager() {
		state = new AnalysisGraph();
		download = new TaskChannel<>("download");
		analyze = new TaskChannel<>("analysis");
		resolve = new TaskChannel<>("dependency resolution");
		bookkeeping = new Bookkeeping();
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
		state.artifactNodes().forEach(this::queueTasksForNode);
	}

	private void queueTasksForNode(AnalysisNode node) {
		queueTaskForNode(node, AnalysisNode::download, download);
		queueTaskForNode(node, AnalysisNode::analysis, analyze);
		queueTaskForNode(node, AnalysisNode::resolution, resolve);
	}

	private static void queueTaskForNode(
			AnalysisNode node,
			Function<AnalysisNode, Computation<?>> getComputation,
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
		processAnswersFromChannel(resolve, state::resolutionOf);
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

	private static ArtifactCoordinates getTaskAndStart(
			TaskChannel<ArtifactCoordinates, ?, ?> channel,
			Function<ArtifactCoordinates, Computation<?>> getComputation)
			throws InterruptedException {
		ArtifactCoordinates artifact = channel.getTask();
		LOGGER.info(format("Starting %s for %s.", channel.taskName(), artifact.coordinates()));
		getComputation.apply(artifact).started();
		return artifact;
	}

	public ArtifactCoordinates getNextToDownload() throws InterruptedException {
		return getTaskAndStart(download, state::downloadOf);
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
		return getTaskAndStart(analyze, state::analysisOf);
	}

	public void analyzed(AnalyzedArtifact artifact) throws InterruptedException {
		LOGGER.info(format("Analysis for %s succeeded: %s", artifact.coordinates(), artifact.violations()));
		analyze.addResult(artifact);
	}

	public void analysisFailed(FailedArtifact artifact) throws InterruptedException {
		LOGGER.info(format("Analysis for %s failed: %s", artifact.coordinates(), artifact.error()));
		analyze.addError(artifact);
	}

	public ArtifactCoordinates getNextToResolve() throws InterruptedException {
		return getTaskAndStart(resolve, state::resolutionOf);
	}

	public void resolved(ResolvedArtifact artifact) throws InterruptedException {
		LOGGER.info(format("Dependency resolution for %s succeeded: %s", artifact.coordinates(), artifact.dependees()));
		resolve.addResult(artifact);
	}

	public void resolutionFailed(FailedArtifact artifact) throws InterruptedException {
		LOGGER.info(format("Dependency resolution for %s failed: %s", artifact.coordinates(), artifact.error()));
		resolve.addError(artifact);
	}

	private class Bookkeeping {

		private boolean running;
		private boolean aborted;

		public void run() {
			startRunning();
			LOGGER.info("Start managing queues.");

			while(!aborted) {
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
