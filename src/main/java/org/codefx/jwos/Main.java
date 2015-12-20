package org.codefx.jwos;

import com.google.common.collect.ImmutableList;
import org.codefx.jwos.analysis.AnalysisFunctions;
import org.codefx.jwos.artifact.AnalyzedArtifact;
import org.codefx.jwos.artifact.ArtifactCoordinates;
import org.codefx.jwos.artifact.DeeplyAnalyzedArtifact;
import org.codefx.jwos.artifact.ProjectCoordinates;
import org.codefx.jwos.artifact.ResolvedArtifact;
import org.codefx.jwos.connect.BlockingReceiver;
import org.codefx.jwos.connect.BlockingSender;
import org.codefx.jwos.connect.Sink;
import org.codefx.jwos.connect.Source;
import org.codefx.jwos.connect.Transformer;
import org.codefx.jwos.connect.TransformerToMany;
import org.codefx.jwos.discovery.ProjectListFile;
import org.codefx.jwos.file.ResultFile;
import org.codefx.jwos.jdeps.JDeps;
import org.codefx.jwos.maven.MavenCentral;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.codefx.jwos.connect.Log.log;

public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	private final BlockingQueue<ProjectCoordinates> mustDetectVersions;
	private final BlockingQueue<ArtifactCoordinates> mustStartAnalysis;
	private final BlockingQueue<ResolvedArtifact> mustStartDependeeAnalysis;
	private final BlockingQueue<ArtifactCoordinates> mustResolve;
	private final BlockingReceiver<ResolvedArtifact> mustAnalyzeArtifactAndStartAnalysingDependees;
	private final BlockingQueue<ResolvedArtifact> mustAnalyze;
	private final BlockingQueue<AnalyzedArtifact> mustDeeplyAnalyze;
	private final BlockingReceiver<DeeplyAnalyzedArtifact> mustFinish;
	private final BlockingQueue<DeeplyAnalyzedArtifact> mustLogResult;
	private final BlockingQueue<DeeplyAnalyzedArtifact> mustWriteResultToFile;

	private final List<Source<ProjectCoordinates>> findProjects;
	private final List<TransformerToMany<ProjectCoordinates, ArtifactCoordinates>> detectVersions;
	private final List<TransformerToMany<ArtifactCoordinates, ArtifactCoordinates>> startAnalysis;
	private final List<TransformerToMany<ResolvedArtifact, ArtifactCoordinates>> startDependeeAnalysis;
	private final List<Transformer<ArtifactCoordinates, ResolvedArtifact>> resolve;
	private final List<Transformer<ResolvedArtifact, AnalyzedArtifact>> analyze;
	private final List<TransformerToMany<AnalyzedArtifact, DeeplyAnalyzedArtifact>> deeplyAnalyze;
	private final List<Sink<DeeplyAnalyzedArtifact>> finish;

	private final ResultFile resultFile;
	private final AnalysisFunctions analysis;
	private final MavenCentral maven;

	public Main() throws IOException {
		mustDetectVersions = new ArrayBlockingQueue<>(5);
		mustStartAnalysis = new ArrayBlockingQueue<>(5);
		// this queue must be nominally unbound because it is part of a cycle, which can lead to deadlock;
		// its size is still bound by the size of 'mustAnalyze' because both queue are added to at the same time
		mustStartDependeeAnalysis = new LinkedBlockingQueue<>();
		mustResolve = new ArrayBlockingQueue<>(5);
		mustAnalyze = new ArrayBlockingQueue<>(5);
		mustAnalyzeArtifactAndStartAnalysingDependees = artifact -> {
			mustAnalyze.put(artifact);
			mustStartDependeeAnalysis.put(artifact);
		};
		mustDeeplyAnalyze = new ArrayBlockingQueue<>(5);
		mustLogResult = new ArrayBlockingQueue<>(5);
		mustWriteResultToFile = new ArrayBlockingQueue<>(5);
		mustFinish = artifact -> {
			mustLogResult.put(artifact);
			mustWriteResultToFile.put(artifact);
		};

		findProjects = new ArrayList<>();
		detectVersions = new ArrayList<>();
		startAnalysis = new ArrayList<>();
		startDependeeAnalysis = new ArrayList<>();
		resolve = new ArrayList<>();
		analyze = new ArrayList<>();
		deeplyAnalyze = new ArrayList<>();
		finish = new ArrayList<>();

		resultFile = ResultFile.read(Util.getPathToResourceFile(Util.RESULT_FILE_NAME));
		analysis = new AnalysisFunctions(resultFile.analyzedArtifactsUnmodifiable());
		maven = new MavenCentral();
	}

	public void run() {
		startMonitoringQueues();
		createAllConnections();
		activateAllConnections();
	}

	private void createAllConnections() {
		createReadProjectFiles(mustDetectVersions::put).forEach(findProjects::add);
		detectVersions.add(createDetectVersions(mustDetectVersions::take, mustStartAnalysis::put, maven));
		startAnalysis.add(createStartAnalysis(mustStartAnalysis::take, mustResolve::put, analysis));
		startDependeeAnalysis.add(createStartDependeeAnalysis(mustStartDependeeAnalysis::take, mustResolve::put, analysis));
		resolve.add(createMavenResolver(mustResolve::take, mustAnalyzeArtifactAndStartAnalysingDependees, maven));
		analyze.add(createJDepsAnalyzer(mustAnalyze::take, mustDeeplyAnalyze::put));
		deeplyAnalyze.add(createDeepAnalyze(mustDeeplyAnalyze::take, mustFinish, analysis));
		finish.add(createLogPrinter(mustLogResult::take));
		finish.add(createResultFileWriter(mustWriteResultToFile::take, resultFile));
	}

	public void activateAllConnections() {
		Stream
				.<List<? extends Runnable>>of(
						findProjects, detectVersions, startAnalysis, resolve, analyze, deeplyAnalyze, finish)
				.flatMap(List::stream)
				.map(Thread::new)
				.forEach(Thread::start);
	}

	private static Stream<Source<ProjectCoordinates>> createReadProjectFiles(
			BlockingReceiver<ProjectCoordinates> out) {
		return Arrays
				.stream(Util.PROJECT_LIST_FILE_NAMES)
				.map(fileName -> createReadProjectFile(fileName, out))
				.filter(Optional::isPresent)
				.map(Optional::get);
	}

	private static Optional<Source<ProjectCoordinates>> createReadProjectFile(
			String fileName, BlockingReceiver<ProjectCoordinates> out) {
		try {
			Logger logger = LoggerFactory.getLogger("Read Project List '" + fileName + "'");
			Path file = Util.getPathToResourceFile(fileName);
			ProjectListFile listFile = new ProjectListFile(file).open();
			Source<ProjectCoordinates> source = new Source<>(
					log(
							listFile::readNextProject,
							"Fetched project %s from " + fileName,
							logger),
					out,
					logger);
			return Optional.of(source);
		} catch (IOException ex) {
			LOGGER.error("Opening '" + fileName + "' failed.", ex);
			return Optional.empty();
		}
	}

	private static TransformerToMany<ProjectCoordinates, ArtifactCoordinates> createDetectVersions(
			BlockingSender<ProjectCoordinates> in,
			BlockingReceiver<ArtifactCoordinates> out,
			MavenCentral maven) {
		Logger logger = LoggerFactory.getLogger("Detect Versions");
		return new TransformerToMany<>(
				in,
				log(
						"Detecting versions of %s.",
						project -> {
							ImmutableList<Version> versions = maven.detectAllVersionsOf(project);
							return project.toArtifactsWithVersions(versions);
						},
						"Detected versions %s.",
						logger),
				out,
				logger);
	}

	private static TransformerToMany<ArtifactCoordinates, ArtifactCoordinates> createStartAnalysis(
			BlockingSender<ArtifactCoordinates> in,
			BlockingReceiver<ArtifactCoordinates> out,
			AnalysisFunctions analysis) {
		Logger logger = LoggerFactory.getLogger("Start To Analyze");
		return new TransformerToMany<>(
				in,
				log(
						"",
						analysis.startAnalysis(),
						"Starting to analyze %s.",
						logger),
				out,
				logger);
	}

	private static TransformerToMany<ResolvedArtifact, ArtifactCoordinates> createStartDependeeAnalysis(
			BlockingSender<ResolvedArtifact> in,
			BlockingReceiver<ArtifactCoordinates> out,
			AnalysisFunctions analysis) {
		Logger logger = LoggerFactory.getLogger("Start To Analyze");
		return new TransformerToMany<>(
				in,
				log(
						"",
						analysis.resolvedDependencies(),
						"Starting to analyze %s.",
						logger),
				out,
				logger);
	}

	private static Transformer<ArtifactCoordinates, ResolvedArtifact> createMavenResolver(
			BlockingSender<ArtifactCoordinates> in, BlockingReceiver<ResolvedArtifact> out, MavenCentral maven) {
		Logger logger = LoggerFactory.getLogger("Maven Resolve");
		return new Transformer<>(
				in,
				log(
						"Downloading %s...",
						maven::downloadMavenArtifact,
						"Downloaded %s.",
						logger),
				out,
				logger);
	}

	private static Transformer<ResolvedArtifact, AnalyzedArtifact> createJDepsAnalyzer(
			BlockingSender<ResolvedArtifact> in, BlockingReceiver<AnalyzedArtifact> out) {
		Logger logger = LoggerFactory.getLogger("JDeps Analysis");
		JDeps jdeps = new JDeps();
		return new Transformer<>(
				in,
				log(
						"Analyzing %s...",
						artifact -> new AnalyzedArtifact(artifact.artifact(), jdeps.analyze(artifact.path())),
						"Analyzed %s.",
						logger),
				out,
				logger);
	}

	private static TransformerToMany<AnalyzedArtifact, DeeplyAnalyzedArtifact> createDeepAnalyze(
			BlockingSender<AnalyzedArtifact> in,
			BlockingReceiver<DeeplyAnalyzedArtifact> out,
			AnalysisFunctions analysis) {
		Logger logger = LoggerFactory.getLogger("Deep Analysis");
		return new TransformerToMany<>(
				in,
				log(
						"Deeply analyzing %s...",
						analysis.deepAnalysis(),
						"Deeply analyzed %s.",
						logger),
				out,
				logger);
	}

	private static Sink<DeeplyAnalyzedArtifact> createLogPrinter(BlockingSender<DeeplyAnalyzedArtifact> in) {
		Logger logger = LoggerFactory.getLogger("Output");
		return new Sink<>(in, artifact -> logger.info("Analysis complete:\n" + artifact.toLongString()), logger);
	}

	private static Sink<DeeplyAnalyzedArtifact> createResultFileWriter(
			BlockingSender<DeeplyAnalyzedArtifact> in,
			ResultFile resultFile) {
		Logger logger = LoggerFactory.getLogger("Result File");
		return new Sink<>(
				in,
				log("Writing %s to result file.",
						artifact -> {
							resultFile.addArtifacts(artifact);
							resultFile.write();
						},
						logger),
				logger);
	}

	private void startMonitoringQueues() {
		new Timer("Queue Statistics").schedule(new QueueStatisticsTimerTask(), 0, 2500);
	}

	public static void main(String[] args) throws Exception {
		Main main = new Main();
		main.run();
	}

	private class QueueStatisticsTimerTask extends TimerTask {

		private static final String STATISTIC_MESSAGE_FORMAT = "\t -> %s: %d\n";

		@Override
		public void run() {
			int detectVersions = mustDetectVersions.size();
			int toAnalyse = mustStartAnalysis.size();
			int resolve = mustResolve.size();
			int analyze = mustAnalyze.size();
			int deeplyAnalyze = mustDeeplyAnalyze.size();
			int logResult = mustLogResult.size();
			int writeResultToFile = mustWriteResultToFile.size();
			String message = "Queue statistics:\n"
					+ format(STATISTIC_MESSAGE_FORMAT, "detectVersions", detectVersions)
					+ format(STATISTIC_MESSAGE_FORMAT, "toAnalyse", toAnalyse)
					+ format(STATISTIC_MESSAGE_FORMAT, "resolve", resolve)
					+ format(STATISTIC_MESSAGE_FORMAT, "analyze", analyze)
					+ format(STATISTIC_MESSAGE_FORMAT, "deeplyAnalyze", deeplyAnalyze)
					+ format(STATISTIC_MESSAGE_FORMAT, "logResult", logResult)
					+ format(STATISTIC_MESSAGE_FORMAT, "writeResultToFile", writeResultToFile);
			LOGGER.info(message);
		}
	}

}
