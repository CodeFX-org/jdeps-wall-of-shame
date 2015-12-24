package org.codefx.jwos.analysis;

import org.codefx.jwos.analysis.state.Computation;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DownloadedArtifact;
import org.codefx.jwos.artifact.FailedArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifact;
import org.codefx.jwos.artifact.IdentifiesArtifactComputation;
import org.codefx.jwos.artifact.ResolvedArtifact;

import java.util.function.Function;

import static org.codefx.jwos.analysis.state.ComputationStateIdentifier.NOT_COMPUTED;

public class AnalysisTaskManager {

	private final AnalysisGraph state;

	private final Channel<ArtifactCoordinates, DownloadedArtifact, FailedArtifact> download;
	private final Channel<ArtifactCoordinates, AnalyzedArtifact, FailedArtifact> analyze;
	private final Channel<ArtifactCoordinates, ResolvedArtifact, FailedArtifact> resolve;

	private final Bookkeeping bookkeeping;

	public AnalysisTaskManager() {
		state = new AnalysisGraph();
		download = new Channel<>();
		analyze = new Channel<>();
		resolve = new Channel<>();
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
			Channel<ArtifactCoordinates, ?, ?> channel) {
		Computation<?> computation = getComputation.apply(node);
		if (computation.state() == NOT_COMPUTED) {
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
			Channel<?, ? extends IdentifiesArtifactComputation<R>, FailedArtifact> channel,
			Function<IdentifiesArtifact, Computation<R>> getComputation) {
		channel.drainResults().forEach(artifact -> getComputation.apply(artifact).succeeded(artifact.result()));
		channel.drainErrors().forEach(artifact -> getComputation.apply(artifact).failed(artifact.result()));
	}

	// QUERY & UPDATE

	private static ArtifactCoordinates getTaskAndStart(
			Channel<ArtifactCoordinates, ?, ?> channel, Function<ArtifactCoordinates, Computation<?>> getComputation)
			throws InterruptedException {
		ArtifactCoordinates artifact = channel.getTask();
		getComputation.apply(artifact).started();
		return artifact;
	}

	public ArtifactCoordinates getNextToDownload() throws InterruptedException {
		return getTaskAndStart(download, state::downloadOf);
	}

	public void downloaded(DownloadedArtifact artifact) throws InterruptedException {
		download.addResult(artifact);
	}

	public void downloadFailed(FailedArtifact artifact) throws InterruptedException {
		download.addError(artifact);
	}

	public ArtifactCoordinates getNextToAnalyze() throws InterruptedException {
		return getTaskAndStart(analyze, state::analysisOf);
	}

	public void analyzed(AnalyzedArtifact artifact) throws InterruptedException {
		analyze.addResult(artifact);
	}

	public void analysisFailed(FailedArtifact artifact) throws InterruptedException {
		analyze.addError(artifact);
	}

	public ArtifactCoordinates getNextToResolve() throws InterruptedException {
		return getTaskAndStart(resolve, state::resolutionOf);
	}

	public void resolved(ResolvedArtifact artifact) throws InterruptedException {
		resolve.addResult(artifact);
	}

	public void resolutionFailed(FailedArtifact artifact) throws InterruptedException {
		resolve.addError(artifact);
	}

	private class Bookkeeping {

		private boolean running;
		private boolean aborted;

		public void run() {
			startRunning();

			while(!aborted) {
				queueTasks();
				processAnswers();
				sleepAndAbortWhenInterrupted();
			}

			stopRunning();
		}

		private void startRunning() {
			if (running)
				throw new IllegalStateException("The bookkeeping thread is already running.");
			running = true;
		}

		private void sleepAndAbortWhenInterrupted() {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				aborted = true;
			}
		}

		private void stopRunning() {
			running = false;
			aborted = false;
		}

		public void abort() {
			aborted = true;
		}
	}

}
